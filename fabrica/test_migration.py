"""Executa o SQL real da migração 4->5 e compara-o ao schema exportado do Room.

Complementa o teste Android; não o substitui.
"""
import json
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app/schemas/com.quemsou.app.data.local.AppDatabase"


def criar(db, version):
    schema = json.loads((SCHEMAS / f"{version}.json").read_text())["database"]
    for table in schema["entities"]:
        db.execute(table["createSql"].replace("${TABLE_NAME}", table["tableName"]))
        for index in table["indices"]:
            db.execute(index["createSql"].replace("${TABLE_NAME}", table["tableName"]))


class MigrationTest(unittest.TestCase):
    def test_upgrade_preserva_dados_e_corresponde_ao_schema_5(self):
        db, esperado = sqlite3.connect(":memory:"), sqlite3.connect(":memory:")
        self.addCleanup(db.close)
        self.addCleanup(esperado.close)
        criar(db, 4)
        criar(esperado, 5)
        db.execute("INSERT INTO baralhos VALUES ('b','Cinema','PERSONAGEM_FILME',1,'EM_DESENVOLVIMENTO','c','Cinema','C')")
        db.execute("INSERT INTO cards VALUES ('c','PESSOA','PERSONAGEM_FILME','Resposta','[]','b')")
        db.execute("INSERT INTO feedback_de_cards VALUES (1,'b','c','BOM','Preservar',1,'ACERTO',2,123)")
        source = (ROOT / "app/src/main/kotlin/com/quemsou/app/data/local/Migrations.kt").read_text(encoding="utf-8")
        bloco = source.split("val MIGRACAO_4_5 =", 1)[1].split("\n}\n", 1)[0]
        sqls = re.findall(r'db.execSQL\("([^"\n]+)"\)', bloco)
        self.assertEqual(len(sqls), 6)
        for sql in sqls:
            db.execute(sql)
        tables = [r[0] for r in esperado.execute("SELECT name FROM sqlite_master WHERE type='table'")]
        for table in tables:
            self.assertEqual(db.execute(f"PRAGMA table_info('{table}')").fetchall(),
                             esperado.execute(f"PRAGMA table_info('{table}')").fetchall(), table)
            self.assertEqual(db.execute(f"PRAGMA foreign_key_list('{table}')").fetchall(),
                             esperado.execute(f"PRAGMA foreign_key_list('{table}')").fetchall(), table)
        self.assertEqual(db.execute("SELECT comentario, contextoJson FROM feedback_de_cards").fetchone(), ("Preservar", ""))
        self.assertEqual(db.execute("SELECT answer, respostaId, bancoDeDicasJson FROM cards").fetchone(), ("Resposta", "", "[]"))
