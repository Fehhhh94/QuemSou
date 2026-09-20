"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const A = require("./acervo.js");
const F = require("./acervo-firestore.js");

function ambiente() {
  const local = {respostaId:"r",resposta:{respostaId:"r",texto:"Resposta"},dicas:{a:{dicaId:"a",texto:"A antiga",escopo:"PUBLICO",status:"ATIVA"}},feedbacks:{porDica:{}}};
  const remoto = {completo:true,resposta:{respostaId:"r",texto:"Resposta",_updateTime:"v1"},dicas:{
    a:{dicaId:"a",texto:"B remota",escopo:"PUBLICO",status:"ATIVA"},
    b:{dicaId:"b",texto:"Segunda",escopo:"PUBLICO",status:"ATIVA"}
  }};
  const ctx = vm.createContext({
    document:{getElementById:()=>null},window:{addEventListener:()=>{},confirm:()=>true},
    estado:{firebase:{configurado:true,config:{}}},AcervoEditorial:A,
    AcervoFirestore:{...F,carregarRespostaRemota:async()=>structuredClone(remoto)},
    fetch:async()=>({ok:true,json:async()=>structuredClone(local)}),
    comCredencialFirebase:async f=>f({},{}),avisar:()=>{},structuredClone,
  });
  vm.runInContext(fs.readFileSync(__dirname+"/acervo-ui.js","utf8"),ctx);
  // A UI real é inspecionada no navegador; aqui testamos as ações assíncronas
  // e seu estado, sem substituir o código responsável por abrir/editar/salvar.
  vm.runInContext(`renderizarListaDeRespostas=()=>{};renderizarEditorDaResposta=()=>{};renderizarStatusRemoto=()=>{};
    globalThis.acervo=estadoDoAcervo;`,ctx);
  return {ctx,remoto,local};
}
test("abrir mostra B remoto; editar b nunca reenvia A local sobre a",async()=>{
  const {ctx,remoto}=ambiente();
  await ctx.abrirRespostaNoAcervo("r");
  assert.equal(ctx.acervo.aberto.dicas.a.texto,"B remota");
  let enviada;
  ctx.AcervoFirestore.salvarRascunhoRemoto=async(_f,_c,_auth,_id,_r,dicas,base)=>{
    enviada=dicas;
    assert.equal(base.resposta._updateTime,"v1");
    remoto.dicas=Object.fromEntries(dicas.map(d=>[d.dicaId,d]));
  };
  assert.equal(await ctx.salvarEdicaoDeDica("b","Segunda revisada","PUBLICO"),true);
  assert.equal(enviada.find(d=>d.dicaId==="a").texto,"B remota");
});
test("conflito preserva texto e base, não recarrega nem tenta salvar automaticamente",async()=>{
  const {ctx}=ambiente();
  await ctx.abrirRespostaNoAcervo("r");
  ctx.acervo.edicoesPendentes.set("a",{texto:"Minha correção",escopo:"PUBLICO"});
  let leituras=0;
  ctx.AcervoFirestore.carregarRespostaRemota=async()=>{leituras++;throw Error("Não deveria ler");};
  ctx.AcervoFirestore.salvarRascunhoRemoto=async()=>{throw new F.ConflitoDeConcorrencia("Mudou");};
  assert.equal(await ctx.salvarEdicaoDeDica("a","Minha correção","PUBLICO"),false);
  assert.equal(leituras,0);
  assert.equal(ctx.acervo.remoto.resposta._updateTime,"v1");
  assert.equal(ctx.acervo.edicoesPendentes.get("a").texto,"Minha correção");
  assert.equal(ctx.acervo.estadoRemoto,"erro");
});
test("troca cancelada não perde edição ou nova dica",async()=>{
  const {ctx}=ambiente();
  await ctx.abrirRespostaNoAcervo("r");
  ctx.acervo.rascunhoDeNovaDica.texto="Não perder";
  ctx.window.confirm=()=>false;
  await ctx.abrirRespostaNoAcervo("outra");
  assert.equal(ctx.acervo.respostaId,"r");
  assert.equal(ctx.acervo.rascunhoDeNovaDica.texto,"Não perder");
});
test("rascunho local divergente só cede ao remoto após decisão explícita",async()=>{
  const {ctx,local}=ambiente();
  local.temRascunhoLocal=true;
  ctx.window.confirm=()=>false;
  await ctx.abrirRespostaNoAcervo("r");
  assert.equal(ctx.acervo.estadoRemoto,"erro");
  assert.equal(ctx.acervo.aberto.dicas.a.texto,"A antiga");
  ctx.window.confirm=()=>true;
  await ctx.abrirRespostaNoAcervo("r");
  assert.equal(ctx.acervo.aberto.dicas.a.texto,"B remota");
});
