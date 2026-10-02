"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");

function ambiente() {
  class Elemento {
    constructor(texto) { this.textContent = texto; this.children = []; this.value = ""; }
    replaceChildren(...nodes) { this.children = []; this.value = ""; nodes.forEach(n => this.append(n)); }
    append(n) { this.children.push(n); if (n.defaultSelected) this.value = n.value; }
    get firstChild() { return this.children[0]; }
  }
  const elementos = {filtroOrigem:new Elemento(), filtroGrupo:new Elemento()};
  const ids = Object.fromEntries(["agrupamentos-disponiveis","novo-grupo","novo-icone","novo-categoria"].map(id=>[id,new Elemento()]));
  const agrupamentos = ["cinema-classico","mundo-da-musica","esportes","conhecimentos-gerais","lugares-e-natureza","especiais"].map((id,i)=>({id,nome:"Grupo "+i,icone:"⭐",categoriaPadrao:id==="especiais"?"ESPECIAIS":"PERSONAGEM_FILME"}));
  const estado = {agrupamentos,origens:[],inventario:[
    {id:"demo",grupoId:"cinema-classico",grupo:"Grupo 0",origem:"asset"},
    {id:"demo",grupoId:"cinema-classico",grupo:"Grupo 0",origem:"catalogo"},
    {id:"tecnico",grupo:"Técnico",tecnico:true},
  ]};
  const ctx = vm.createContext({estado,elementos,document:{getElementById:id=>ids[id]},criar:(_tag,texto)=>new Elemento(texto)});
  const source=fs.readFileSync(__dirname+"/app.js","utf8");
  vm.runInContext(source.slice(source.indexOf("function montarFiltros()"),source.indexOf("function renderizarAvisos()")),ctx);
  return {ctx,ids,elementos};
}

test("seis opções incluem vazios e excluem grupo técnico do filtro comum",()=>{
  const {ctx,ids,elementos}=ambiente();
  ctx.montarFiltros();ctx.montarAgrupamentos();
  assert.equal(elementos.filtroGrupo.children.length,7); // Todos + seis
  assert.equal(ids["novo-grupo"].children.length,6);
  assert.equal(ids["novo-grupo"].value,"especiais");
  assert.equal(ids["novo-categoria"].value,"ESPECIAIS");
  assert.match(ids["agrupamentos-disponiveis"].children[0].textContent,/1 baralho/);
  assert.match(ids["agrupamentos-disponiveis"].children[2].textContent,/sem baralhos/);
});

test("recarregar inventário preserva escolha e categoria em edição",()=>{
  const {ctx,ids}=ambiente();ctx.montarAgrupamentos();
  ids["novo-grupo"].value="esportes";
  ids["novo-categoria"].value="MUNDO_DA_MUSICA";
  ctx.montarAgrupamentos();
  assert.equal(ids["novo-grupo"].value,"esportes");
  assert.equal(ids["novo-categoria"].value,"MUNDO_DA_MUSICA");
});

test("trocar agrupamento aplica ícone e categoria padrão",()=>{
  const {ctx,ids}=ambiente();ctx.montarAgrupamentos();
  ids["novo-grupo"].value="esportes";ctx.ajustarNovoAgrupamento();
  assert.equal(ids["novo-categoria"].value,"PERSONAGEM_FILME");
  assert.equal(ids["novo-icone"].value,"⭐");
});
