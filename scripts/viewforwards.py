import requests

url = "https://discord.com/api/v10/applications/1290520169185280062/commands"

json = {
    "name": "viewforwards",
    "type": 1,
    "description": "View all forwards playing that day"
}

# For authorization, you can use either your bot token
headers = {
    "Authorization": "Bot MTI5MDUyMDE2OTE4NTI4MDA2Mg.GTf6eM.1tlwZTfU-0eNdSHZkDDuPtKVOP_Ax9aHnrvA0k"
}

r = requests.put(url, headers=headers, json=json)
print(r.json())