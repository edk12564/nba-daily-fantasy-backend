import requests
import os
from dotenv import load_dotenv, find_dotenv

load_dotenv(find_dotenv())
VITE_DISCORD_CLIENT_ID = os.getenv("VITE_DISCORD_CLIENT_ID")
BOT_ACCESS_KEY = os.getenv("BOT_ACCESS_KEY")

url = f"https://discord.com/api/v10/applications/{VITE_DISCORD_CLIENT_ID}/commands"

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
    f"Authorization": "Bot {BOT_ACCESS_KEY}"
}

r = requests.post(url, headers=headers, json=json)
print(r.json())
