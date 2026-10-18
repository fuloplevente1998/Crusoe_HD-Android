#!/usr/bin/env python3
"""Reproducible, read-only extraction of this supplied J2ME build. Never executes it."""
import argparse, collections, hashlib, json, struct, zipfile
from pathlib import Path

LANGUAGES = ['en', 'fr', 'it', 'de', 'es']

def strings(data, offset, count):
    result = []
    for _ in range(count):
        chars = []
        while data[offset]:
            value = data[offset]; offset += 1
            code = value & 127
            if value & 128:
                value = data[offset]; offset += 1
                code |= (value & 127) << 7
                if value & 128:
                    code |= data[offset] << 14; offset += 1
            chars.append(chr(code))
        offset += 1
        result.append(''.join(chars))
    return result

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')

def extract(jar, out):
    with zipfile.ZipFile(jar) as archive:
        entries = archive.infolist()
        if sum(e.file_size for e in entries) > 64 * 1024 * 1024:
            raise ValueError('Unexpectedly large archive; stopped before extraction.')
        data = archive.read('data/D0')
        if len(data) != 80157:
            raise ValueError('This extractor is for the supplied S60v5 1.0.0 build, with an 80157-byte D0.')
        banks = {}
        offsets = []
        for bank in range(4):
            base = 80049 + bank * 27
            count = struct.unpack_from('<H', data, base)[0]
            for language, code in enumerate(LANGUAGES):
                file_id, offset = struct.unpack_from('<BI', data, base + 2 + language * 5)
                assert file_id == 0
                banks.setdefault(code, []).append(strings(data, offset, count))
                offsets.append(dict(bank=bank, language=code, offset=offset, count=count))
        for language, language_banks in banks.items():
            write(out / 'strings' / (language + '.json'), dict(ui_and_tutorial=language_banks[0], names=language_banks[1], descriptions_0_59=language_banks[2], descriptions_60_119=language_banks[3]))
        names = banks['en'][1]
        rows = [line.split(';') for line in archive.read('objects.csv').decode('ascii').splitlines() if line and not line.startswith('@')]
        assert len(rows) == 13 and all(len(row) == 120 for row in rows)
        def ingredients(value):
            if value == '-1': return []
            return [dict(id=int(pair.split('|')[0]), name=names[int(pair.split('|')[0])], quantity=int(pair.split('|')[1])) for pair in value.split('#')]
        items = []
        for i in range(120):
            items.append(dict(id=i, names={lang:banks[lang][1][i] for lang in LANGUAGES}, descriptions={lang:banks[lang][2 + i//60][i%60] for lang in LANGUAGES}, raw_columns=[row[i] for row in rows], ingredients=ingredients(rows[8][i]), tools=ingredients(rows[9][i]), crafting_context_id=int(rows[10][i]), clothing_slot=int(rows[11][i]), category_code=int(rows[12][i]), placeholder=i==95))
        recipes = [dict(id=item['id'], name=item['names']['en'], ingredients=item['ingredients'], tools=item['tools'], context_id=item['crafting_context_id'], context_name=names[item['crafting_context_id']] if item['crafting_context_id'] >= 0 else None) for item in items if item['ingredients']]
        maps = {}
        for layer in ['FULL', 'Borders', 'Decor', 'Objects', 'Rocks']:
            values = list(archive.read('BIGisland' + layer + '.byt'))
            assert len(values) == 180 * 180
            maps[layer] = values
            write(out / 'maps' / (layer + '.json'), dict(width=180, height=180, values=values))
        event_definitions = [
            (50, 'Ébredés a szigeten', [0], 'aj'), (51, 'Az Amber Queen hajótörése', [1], 'ak'),
            (52, 'Tárgyvizsgálat', [2,3,4], 'al'), (53, 'Tárgykészítés', [5,6,7], 'am'),
            (54, 'Pihenés és alvás', [8,9,10], 'an'), (55, 'Kókusz ledobása kővel', [11,12,13], 'ao'),
            (56, 'Sekélyvízi hálós halászat', [14,15,16], 'ap'), (57, 'Mélyvízi horgászat', [17,18,19], 'aq'),
            (58, 'Vadászat íjjal', [20,21,22], 'ar'), (59, 'Táborépítés a zászlónál', [23,24,25], 'as'),
            (60, 'Ruházat', [26,27,28], 'at'), (61, 'A roncs maradványainak összeszedése', [29], 'au'),
            (62, 'Árapály és időjárás', [30,31,32], 'av')
        ]
        events = [dict(tile_code=code, title=title, ui_text_indices=indices, english_texts=[banks['en'][0][index] for index in indices], once_flag='p.var_boolean_'+flag, trigger_positions=[dict(x=i%180,y=i//180) for i,value in enumerate(maps['Rocks']) if value==code], evidence='data/g.java, method j(): reads Rocks[(player_y-1)*180+(player_x-1)], checks per-event once flag') for code,title,indices,flag in event_definitions]
        write(out / 'events.json', events)
        frame_counts=struct.unpack_from('<15H',data,0)
        frames=[];frame_offset=30
        for bank,count in enumerate(frame_counts):
            for index in range(count):
                x,y,width_raw,height,anchor_x,anchor_y=struct.unpack_from('<6H',data,frame_offset);frame_offset+=12
                frames.append(dict(bank=bank,index=index,x=x,y=y,width_raw=width_raw,height=height,anchor_x=anchor_x,anchor_y=anchor_y))
        write(out / 'sprite-metadata.json', dict(bank_frame_counts=list(frame_counts),frames=frames,note='Raw six-short sprite records from data/w.java loader. Width may carry transformation flags; these are retained unmodified.'))
        positions = [dict(tile_x=i%180,tile_y=i//180,stored_value=value,item_id=value-1,name=names[value-1]) for i,value in enumerate(maps['Objects']) if value and value<=120]
        write(out / 'items.json', items)
        write(out / 'recipes.json', recipes)
        write(out / 'object-positions.json', positions)
        write(out / 'string-offsets.json', offsets)
        write(out / 'provenance.json', dict(filename=jar.name, sha256=hashlib.sha256(jar.read_bytes()).hexdigest(), item_entries=len(items), placeholder_entries=1, recipes=len(recipes), languages=LANGUAGES, strings_per_language=sum(len(b) for b in banks['en']), map_layers=5, map_size=[180,180], class_files=sum(e.filename.endswith('.class') for e in entries), original_object_placements=len(positions), tutorial_event_types=len(events),sprite_banks=15,sprite_records=len(frames),encoding='NUL-terminated strings, unsigned 7-bit continuation encoding for Java char values; little-endian bank offset table at D0 byte 80048', source_evidence=['data/ac.java: string-bank reader','data/y.java: objects.csv parser','data/r.java: ingredients and tool requirements','data/aa.java: tent/cabin crafting filters','data/g.java j(): 13 once-only tile-triggered tutorial events','data/a.java G(): ship and camp reconstruction transitions']))
        lines = ['# Kinyert eredeti receptek', '', '| ID | Eredmény | Felhasznált alapanyagok | Szükséges eszközök | Környezetkód |', '|---|---|---|---|---|']
        def pretty(entries): return ', '.join(f"{v['quantity']} × {v['name']}" for v in entries) or '—'
        for r in recipes:
            lines.append(f"| {r['id']} | {r['name']} | {pretty(r['ingredients'])} | {pretty(r['tools'])} | {r['context_name'] or 'Bárhol'} |")
        (out / 'RECIPES.md').write_text('\n'.join(lines)+'\n')
        # A data visualization of the original layers; no sprite copying or JAR execution.
        try:
            from PIL import Image, ImageDraw
            colors=['#dfe187','#65942e','#cc885d','#5b5e22','#66f5d7','#41d4e1','#1fb1f0','#54493c','#bcad6c','#24501b','#945d3b','#302b14','#106272','#0d5f75','#004a85','#312522']
            image=Image.new('RGB',(720,720));draw=ImageDraw.Draw(image)
            for i,value in enumerate(maps['FULL']):
                x,y=(i%180)*4,(i//180)*4;draw.rectangle((x,y,x+3,y+3),fill=colors[value%len(colors)])
            for p in positions:
                if p['item_id'] in [18,76,77,78,79,80,22,23,84,89,92,96,97]:
                    x,y=p['tile_x']*4,p['tile_y']*4;draw.ellipse((x-3,y-3,x+3,y+3),fill='#f8df91')
            image.save(out / 'original-map-data.png')
        except ImportError:
            pass
        print(f"Extracted {len(items)} item entries, {len(recipes)} recipes, {len(LANGUAGES)} languages and {len(maps)} map layers to {out}.")

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', type=Path);parser.add_argument('--out',type=Path,default=Path('reference/extracted'))
    args=parser.parse_args();extract(args.jar,args.out)
