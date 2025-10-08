import requests
import os
from dotenv import load_dotenv, find_dotenv

load_dotenv(find_dotenv())
VITE_DISCORD_CLIENT_ID = os.getenv("VITE_DISCORD_CLIENT_ID")
BOT_ACCESS_KEY = os.getenv("BOT_ACCESS_KEY")

url = f"https://discord.com/api/v10/applications/{VITE_DISCORD_CLIENT_ID}/commands"

json = {
    "name": "info",
    "type": 1,
    "description": "Get available application commands"
}

# For authorization, you can use either your bot token
headers = {
    f"Authorization": "Bot {BOT_ACCESS_KEY}"
}

r = requests.post(url, headers=headers, json=json)
print(r.json())