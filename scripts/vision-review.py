import base64, json, subprocess, os, sys, urllib.request

KEY = subprocess.run(['security','find-generic-password','-s','Airgen OpenRouter Key','-w'],
                     capture_output=True, text=True).stdout.strip()
if not KEY: sys.exit("no key")

D = "docs/screenshots"
# A representative spread: the hero surfaces, the sheets where decisions happen,
# the states, and the ones I could see were visually broken.
PICK = ["live_drive_screen.png","track_evidence_screen.png","drive_review_sheet.png",
        "journey_guide_sheet.png","permission_primer_sheet.png","pause_reason_sheet.png",
        "odometer_discrepancy_sheet.png","odometer_rejection_sheet.png","office_picker_sheet.png",
        "saved_tracks_screen.png","track_detail_screen.png","profile_screen.png"]
PICK = [p for p in PICK if os.path.exists(os.path.join(D,p))]

content = [{"type":"text","text": open('/tmp/vision_brief.txt').read()}]
for p in PICK:
    b = base64.b64encode(open(os.path.join(D,p),'rb').read()).decode()
    content.append({"type":"text","text": f"\n--- {p} ---"})
    content.append({"type":"image_url","image_url":{"url": f"data:image/png;base64,{b}"}})

req = urllib.request.Request(
    "https://openrouter.ai/api/v1/chat/completions",
    data=json.dumps({"model": sys.argv[1],
                     "messages":[{"role":"user","content":content}],
                     "max_tokens": 16000}).encode(),
    headers={"Authorization": f"Bearer {KEY}", "Content-Type":"application/json"})
r = json.loads(urllib.request.urlopen(req, timeout=600).read())
if "choices" not in r: sys.exit(json.dumps(r)[:400])
print(r["choices"][0]["message"]["content"])
u = r.get("usage",{})
sys.stderr.write(f"\n[{sys.argv[1]} · {u.get('prompt_tokens')} in / {u.get('completion_tokens')} out]\n")
