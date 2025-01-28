import requests

url = "https://discord.com/api/v10/applications/1290520169185280062/commands"

# This is an example CHAT_INPUT or Slash Command, with a type of 1
json = {
        "name": "setroster",
        "type": 1,
        "description": "Set your roster for the day",
        "options": [
            {
                "name": "position",
                "description": "Roster position you want to set",
                "type": 3,  # Type 3 is STRING
                "required": True,
                "choices": [
                    {"name": "G", "value": "G"},
                    {"name": "F", "value": "F"},
                    {"name": "C", "value": "C"}
                ]
            }
        ]
    }

# For authorization, you can use either your bot token
headers = {
    "Authorization": "Bot MTI5MDUyMDE2OTE4NTI4MDA2Mg.GTf6eM.1tlwZTfU-0eNdSHZkDDuPtKVOP_Ax9aHnrvA0k"
}

r = requests.put(url, headers=headers, json=json)
print(r.json())
