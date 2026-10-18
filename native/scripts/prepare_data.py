#!/usr/bin/env python3
"""Build the native game's local data pack from an existing private extraction.
No original executable bytecode is read or included. Keep this pack private.
"""
import argparse, hashlib, io, json, struct
from pathlib import Path

def pack(source, output, translations=None):
    load=lambda name:json.loads((source/name).read_text())
    items, recipes, events=load('items.json'),load('recipes.json'),load('events.json')
    names=json.loads(translations.read_text()) if translations and translations.exists() else [i['names']['en'] for i in items]
    assert len(names)==120
    assert len(items)==120 and len(recipes)==45 and len(events)==13
    b=io.BytesIO()
    def integer(n):b.write(struct.pack('>i',int(n)))
    def text(s):
        data=s.encode('utf-8');assert len(data)<65536 and b'\0' not in data
        b.write(struct.pack('>H',len(data)));b.write(data)
    integer(0x43525332);text(load('provenance.json')['sha256']);integer(180);integer(180)
    for name in ['FULL','Borders','Decor','Objects','Rocks']:
        values=load('maps/'+name+'.json')['values'];assert len(values)==32400
        b.write(bytes(values))
    integer(len(items))
    for i in items:
        text(names[i['id']]);text(i['names']['en']);text(i['descriptions']['en'])
        for v in i['raw_columns'][:8]:integer(v)
        integer(i['clothing_slot']);integer(i['category_code'])
    integer(len(recipes))
    for r in recipes:
        integer(r['id']);integer(r['context_id'])
        for group in [r['ingredients'],r['tools']]:
            integer(len(group))
            for pair in group:integer(pair['id']);integer(pair['quantity'])
    integer(len(events))
    for e in events:text(e['title']);text('\n'.join(e['english_texts']));integer(e['tile_code'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(b.getvalue())
    print(json.dumps({'output':str(output),'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'items':120,'recipes':45,'map_layers':5}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('extracted',type=Path);p.add_argument('--translations',type=Path,default=Path('private/hu-items.json'));p.add_argument('--output',type=Path,default=Path('app/src/main/assets/world.pack'));a=p.parse_args();pack(a.extracted,a.output,a.translations)
