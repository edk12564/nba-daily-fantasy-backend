# NBA Daily Fantasy (Backend)

Because someone needs to manage all those "LeBron is washed" debates with actual data.

# What does this program do?

This is the backend for a Discord activity that runs NBA daily fantasy sports competitions. Think of it as the brain behind your Discord server's basketball obsession, tracking players, manages rosters, calculates scores, and probably judging your terrible draft picks.

# Features

- Daily Roster Management: Set your lineup before tip-off
- Position-based Drafting: PG, SG, SF, PF, C 
- Budget System: $100 salary cap to keep things interesting 
- Leaderboard: Serverwide and Global leaderboards
- Discord Integration: Slash commands or setup a frontend using Discord Activites
- Lock Times: Once the games start, you're locked in. No further changes can be made

# Tech Stack

- Java 21 - Because we're not savages using Java 8
- Spring Boot 3.3.2 - The framework that makes Java not suck
- PostgreSQL - Database that actually works (unlike MySQL)
- Discord API - For all your bot and activity needs
- OkHttp - HTTP client for Discord Activities integration
- Maven - Dependency management that doesn't make you want to cry

# Architecture

src/main/java/com/bigschlong/demo/
controllers/     # REST endpoints.. 
services/        # Business logic.. 
repositories/    # Database access.. 
models/          # Data Transfer Objects.. 
interceptors/    # Interceptors for Security and Authentication..
utils/           # Helper functions.. 

# Getting Started

# Prerequisites
- Java 21 (or higher, if you're feeling adventurous)
- PostgreSQL database
- Discord API Key
- A sense of humor (optional but recommended)

# Setup
1. Clone this bad boy
2. Set up your environment variables:
   ```bash
   DB_PASSWORD=db_password
   VITE_DISCORD_CLIENT_ID=discord_client_id
   DISCORD_CLIENT_SECRET=discord_secret
   ```
3. Setup slash commands using /scripts or make your own 
4. Deploy and Run
5. Watch the magic happen

# Database
The app expects a PostgreSQL database. It'll validate the schema on startup, so make sure your tables are set up correctly.

# API Endpoints

# Core Fantasy Endpoints
- `GET /api/activity/todays-players` - Get today's available players
- `GET /api/activity/my-roster/{guildId}/{discordPlayerId}` - View your roster
- `POST /api/activity/my-roster` - Set a player in your roster
- `GET /api/activity/rosters/{guildId}` - Server leaderboard
- `GET /api/activity/lock-time` - Check if rosters are locked

# Discord Integration
- `POST /api/activity/token` - Discord OAuth token exchange
- `GET /discord-players/{id}/roster` - Get roster by Discord ID

# Discord Bot Commands

The bot includes several prebuilt slash commands:
- `/setroster` - Set your daily lineup
- `/myroster` - View your current roster
- `/leaderboard` - See who's winning (and losing)
- `/viewplayers` - Browse available players by position

# Scripts

The `scripts/` folder contains Python utilities for setting your Discord slash commands. Feel free to add your own!

# Discord Activities Frontend Integration

This backend is designed to work seamlessly with a Discord Activities frontend. Here's how the integration works:

OkHttp Integration
- The backend uses OkHttp as its HTTP client to communicate with Discord's API endpoints. This enables OAuth2 Token Exchange to allow for authentication to work through Discord API

Frontend-Backend Communication
- RESTful API: Clean endpoints that your Discord Activities frontend can consume
- Real-time Updates: Backend processes Discord interactions and updates the database accordingly
- State Management: Maintains user rosters, leaderboards, and game state

Key Integration Points
- Token Endpoint: `/api/activity/token` - Exchanges Discord authorization codes for access tokens
- User Management: Tracks Discord users across different servers (guilds)
- Activity State: Manages the current state of fantasy competitions
- Data Persistence: Stores all user choices and game data in PostgreSQL

Testing

There's a `test.http` file for testing endpoints. Or you can use Postman if you're fancy.

# Extra Notes

- Positions: You need one player for each position (PG, SG, SF, PF, C)
- Discord Integration: Requires proper bot setup and OAuth2 configuration

# Contributing

Found a bug? Want to add a feature? Feel free to contribute! Just make sure your code doesn't make the existing codebase explode. That would be preferred.
