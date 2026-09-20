// Opt-in, exclusivamente em projeto demo e loopback. Não lê configuração real.
// QUEMSOU_ACERVO_EMULADOR=1 node --test administrador/estaticos/acervo-emulador.test.js
"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const F = require("./acervo-firestore.js");
const A = require("./acervo.js");
const config = { projectId: "demo-quemsou-acervo", baseUrl: "http://127.0.0.1:8788/v1" };
const raiz = config.baseUrl + "/" + F.raizDeDocumentos(config);
function token(uid) {
  const json = v => Buffer.from(JSON.stringify(v)).toString("base64url");
  return { idToken: json({ alg: "none", typ: "JWT" }) + "." + json({
    sub: uid, user_id: uid, aud: config.projectId, iss: "https://securetoken.google.com/" + config.projectId,
    iat: Math.floor(Date.now()/1000), exp: Math.floor(Date.now()/1000)+3600,
    firebase: { sign_in_provider: "anonymous", identities: {} }
  }) + "." };
}
const admin = token("qa-admin");
function resposta(id) { return {schemaVersion:1, respostaId:id, texto:"Resposta de teste", tipo:"COISA", origem:"EDITORIAL"}; }
function dica(i) { return {schemaVersion:1,dicaId:"fato-"+i,texto:"Texto original "+i,escopo:"PUBLICO",status:"ATIVA",origem:"EDITORIAL",revisaoTecnica:1}; }
async function localFetch(url, opcoes) {
  assert.equal(new URL(url).origin, "http://127.0.0.1:8788");
  assert.ok(url.includes("/projects/demo-quemsou-acervo/"));
  return fetch(url, opcoes);
}
const ler = id => F.carregarRespostaRemota(localFetch, config, admin, id);
const salvar = (id, r, dicas, base) => F.salvarRascunhoRemoto(localFetch, config, admin, id, r, dicas, base);

test("acervo REST + Rules reais no emulador", {skip: process.env.QUEMSOU_ACERVO_EMULADOR !== "1"}, async t => {
  const seed = await localFetch(raiz + "/admins/qa-admin", {method:"PATCH",headers:{Authorization:"Bearer owner","Content-Type":"application/json"},body:JSON.stringify({fields:F.camposFirestore({ativo:true})})});
  assert.ok(seed.ok, await seed.text());
  const prefixo = "qa-" + crypto.randomUUID();
  const id = prefixo + " / ação ._%";
  let base;
  await t.test("500 dicas em commit atômico, paginação e releitura", async () => {
    await salvar(id, resposta(id), Array.from({length:500}, (_,i)=>dica(i)), null);
    base = await ler(id);
    assert.equal(Object.keys(base.dicas).length,500);
    assert.equal(base.completo,true);
    assert.equal(base.resposta.respostaId,id);
  });
  await t.test("editar uma dica grava somente pai + dica e preserva vizinhas", async () => {
    assert.ok(base);
    let quantidade;
    const observar = (url, opcoes) => {
      if (url.endsWith(":commit")) quantidade=JSON.parse(opcoes.body).writes.length;
      return localFetch(url,opcoes);
    };
    const banco=Object.values(base.dicas).map(d=>d.dicaId==="fato-1"?{...d,texto:"Correção remota B"}:d);
    await F.salvarRascunhoRemoto(observar,config,admin,id,base.resposta,banco,base);
    assert.equal(quantidade,2);
    const depois=await ler(id);
    assert.equal(depois.dicas["fato-2"]._updateTime,base.dicas["fato-2"]._updateTime);
    assert.notEqual(depois.resposta._updateTime,base.resposta._updateTime);
    await assert.rejects(salvar(id,base.resposta,banco,base),F.ConflitoDeConcorrencia);
    base=depois;
  });
  await t.test("501 recusada antes de qualquer rede", async () => {
    let chamadas=0;
    await assert.rejects(F.salvarRascunhoRemoto(()=>{chamadas++;},config,admin,id,base.resposta,[...Object.values(base.dicas),dica(500)],base),/500/);
    assert.equal(chamadas,0);
  });
  await t.test("ids . e _. e caracteres especiais têm documentos diferentes", async () => {
    for (const sufixo of [".","_.","..","_..","__x__","ação / %"]) {
      const rid=prefixo+sufixo;
      await salvar(rid,resposta(rid),[{...dica(1),dicaId:sufixo}],null);
      const lido=await ler(rid);
      assert.equal(lido.dicas[sufixo].dicaId,sufixo);
      await salvar(rid,lido.resposta,[{...lido.dicas[sufixo],texto:"Editada "+sufixo}],lido);
      assert.equal((await ler(rid)).dicas[sufixo].texto,"Editada "+sufixo);
    }
  });
  await t.test("migração falha antes do commit, retoma e não reverte correção remota", async () => {
    const rid=prefixo+"-migracao";
    const previa={respostas:{[rid]:resposta(rid)},dicas:{[rid]:{a:{...dica(1),dicaId:"a"},b:{...dica(2),dicaId:"b"}}}};
    const falha=(url,op)=>{if(url.endsWith(":commit"))throw Error("Falha de rede simulada");return localFetch(url,op);};
    assert.equal((await F.migrarPreviaParaRemoto(falha,config,admin,previa,[rid])).erros.length,1);
    assert.equal(await ler(rid),null);
    assert.equal((await F.migrarPreviaParaRemoto(localFetch,config,admin,previa,[rid])).criadas.length,1);
    const remoto=await ler(rid);
    await salvar(rid,remoto.resposta,[{...remoto.dicas.a,texto:"B corrigida remotamente"},remoto.dicas.b],remoto);
    assert.equal((await F.migrarPreviaParaRemoto(localFetch,config,admin,previa,[rid])).jaExistentes.length,1);
    assert.equal((await ler(rid)).dicas.a.texto,"B corrigida remotamente");
  });
  await t.test("recupera pai incompleto antigo sem sobrescrever dica corrigida", async () => {
    const rid=prefixo+"-parcial";
    const put=async(caminho,campos)=>{
      const r=await localFetch(raiz+"/"+caminho,{method:"PATCH",headers:{Authorization:"Bearer owner","Content-Type":"application/json"},body:JSON.stringify({fields:F.camposFirestore(campos)})});
      assert.ok(r.ok,await r.text());
    };
    await put(F.caminhoDaResposta(rid),{...resposta(rid),respostaId:A.idDoDocumento(rid),revisaoTecnica:1});
    await put(F.caminhoDaDica(rid,"fato-1"),{...dica(1),dicaId:A.idDoDocumento("fato-1"),texto:"Correção preservada"});
    assert.equal((await ler(rid)).completo,false);
    const previa={respostas:{[rid]:resposta(rid)},dicas:{[rid]:Object.fromEntries([dica(1),dica(2)].map(d=>[d.dicaId,d]))}};
    const resultado=await F.migrarPreviaParaRemoto(localFetch,config,admin,previa,[rid]);
    assert.deepEqual(resultado.erros,[]);
    const final=await ler(rid);
    assert.equal(final.completo,true);
    assert.equal(final.dicas["fato-1"].texto,"Correção preservada");
  });
  await t.test("Rules negam leitor, alteração isolada, delete e 501 identidades", async () => {
    const url=raiz+"/"+F.caminhoDaDica(id,"fato-1");
    const comum=await localFetch(url,{headers:{Authorization:"Bearer "+token("qa-leitor").idToken}});
    assert.equal(comum.status,403);
    const dados={...base.dicas["fato-1"],dicaId:A.idDoDocumento("fato-1"),revisaoTecnica:99};delete dados._updateTime;
    const isolada=await localFetch(url,{method:"PATCH",headers:{Authorization:"Bearer "+admin.idToken,"Content-Type":"application/json"},body:JSON.stringify({fields:F.camposFirestore(dados)})});
    assert.equal(isolada.status,403);
    const apagar=await localFetch(url,{method:"DELETE",headers:{Authorization:"Bearer "+admin.idToken}});
    assert.equal(apagar.status,403);
    const pai={...base.resposta,respostaId:A.idDoDocumento(id),revisaoTecnica:99,dicaIds:Array.from({length:501},(_,i)=>A.idDoDocumento("fato-"+i))};delete pai._updateTime;
    const excesso=await localFetch(raiz+"/"+F.caminhoDaResposta(id),{method:"PATCH",headers:{Authorization:"Bearer "+admin.idToken,"Content-Type":"application/json"},body:JSON.stringify({fields:F.camposFirestore(pai)})});
    assert.equal(excesso.status,403);
  });
});
