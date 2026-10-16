#!/usr/bin/env python3
"""Crea .env sin sobrescribir uno existente ni imprimir secretos."""
from pathlib import Path
import secrets,base64,os
root=Path(__file__).resolve().parent.parent
text=(root/'.env.example').read_text(encoding='utf-8')
values={'DB_PASSWORD':secrets.token_hex(24),'DB_ROOT_PASSWORD':secrets.token_hex(32),'JWT_SECRET':secrets.token_hex(48),'MP_ENCRYPTION_KEY':base64.b64encode(secrets.token_bytes(32)).decode()}
for key,value in values.items():text=text.replace(key+'=\n',key+'='+value+'\n')
try:
 fd=os.open(root/'.env',os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
 with os.fdopen(fd,'w',encoding='utf-8') as out:out.write(text)
 print('.env creado. Completá dominio, datos legales y servicios que vayas a usar.')
except FileExistsError:
 raise SystemExit('Ya existe .env; no se modificó.')
