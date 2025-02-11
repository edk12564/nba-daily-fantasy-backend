

import requests

url = "https://discord.com/api/v10/applications/1290520169185280062/commands"
headers = {
    "Authorization": "Bot MTI5MDUyMDE2OTE4NTI4MDA2Mg.GTf6eM.1tlwZTfU-0eNdSHZkDDuPtKVOP_Ax9aHnrvA0k"
}

# this doesnt work because you get rate limited by discord. delete manually below.

# # Get all existing commands
# response = requests.get(url, headers=headers)
# if response.status_code != 200:
#     print(f"Failed to fetch commands: {response.status_code} - {response.text}")
#     exit()
#
# commands = response.json()
#
# # Delete each command
# for command in commands:
#     delete_url = f"{url}/{command['id']}"
#     delete_response = requests.delete(delete_url, headers=headers)
#     if delete_response.status_code == 204:
#         print(f"Deleted command: {command['name']}")
#     else:
#         print(f"Failed to delete command {command['name']}: {delete_response.status_code} - {delete_response.text}")


# delete_url = f"{url}/leaderboard"
# delete_response = requests.delete(delete_url, headers=headers)

# delete_url = f"{url}/myroster"
# delete_response = requests.delete(delete_url, headers=headers)

# delete_url = f"{url}/info"
# delete_response = requests.delete(delete_url, headers=headers)

# delete_url = f"{url}/viewpointguards"
# delete_response = requests.delete(delete_url, headers=headers)

# delete_url = f"{url}/viewsmallforwards"
# delete_response = requests.delete(delete_url, headers=headers)

# delete_url = f"{url}/viewshootingguards"
# delete_response = requests.delete(delete_url, headers=headers)

# delete_url = f"{url}/viewpowerforwards"
# delete_response = requests.delete(delete_url, headers=headers)


