import requests

url = "https://discord.com/api/v10/applications/1290520169185280062/commands"

# This is an example CHAT_INPUT or Slash Command, with a type of 1
json = {
        "name": "setroster",
        "type": 1,
        "description": "Set your roster for today",
        "options": [
            {
                "name": "position",
                "description": "Roster position you want to set",
                "type": 3,  # Type 3 is STRING
                "required": True,
                "choices": [
                    {"name": "PG", "value": "PG"},
                    {"name": "SG", "value": "SG"},
                    {"name": "PF", "value": "PF"},
                    {"name": "SF", "value": "SF"},
                    {"name": "C", "value": "C"}
                ]
            }
        ]
    }

# For authorization, you can use either your bot token
headers = {
    "Authorization": "Bot MTI5MDUyMDE2OTE4NTI4MDA2Mg.GTf6eM.1tlwZTfU-0eNdSHZkDDuPtKVOP_Ax9aHnrvA0k"
}

r = requests.post(url, headers=headers, json=json)
print(r.json())
