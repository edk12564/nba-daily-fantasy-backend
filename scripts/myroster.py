import requests

url = "https://discord.com/api/v10/applications/1290520169185280062/commands"

json = {
    "name": "myroster",
    "type": 1,
    "description": "Get your roster for today"
}

# For authorization, you can use either your bot token
headers = {
    "Authorization": "Bot MTI5MDUyMDE2OTE4NTI4MDA2Mg.GTf6eM.1tlwZTfU-0eNdSHZkDDuPtKVOP_Ax9aHnrvA0k"
}

r = requests.post(url, headers=headers, json=json)
print(r.json())