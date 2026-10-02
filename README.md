# EventsLib

A modern, high-performance **Events Management Library** for Minecraft Paper 1.21.1+ servers.

## Features
- **54-Slot GUI Layouts**: Full interactive inventory layouts for previews, showcases, and drag-and-drop admin editing.
- **Privacy Controls**: Events can be individually toggled between `PRIVATE` and `PUBLIC`.
- **Custom Model Data Support**: Supports numeric and custom resource-pack string identifiers (`/el model <id> <string>`).
- **Daily Login Rewards (3:00 PM SGT)**: Automatic daily reward tracking resetting strictly at **3:00 PM Singapore Time (UTC+8)** with login reminder notifications.
- **Event Shop System**: Collect and exchange custom event collectible items for exclusive admin-configured rewards.
- **Event Spin Wheel (Shards Integration)**:
  - Animated horizontal roulette rolling wheel.
  - Native integration with the **Shards** plugin (`ShardsService`).
  - Fully configurable prize items with individual probability weights and drop chance calculations.
- **SQLite Database Persistence**: Asynchronous, thread-safe database storage for events, layouts, shop items, spin pools, and daily claims.

## Commands
| Command | Permission | Description |
|---|---|---|
| `/el` | Default | Opens the Server Events Directory GUI |
| `/el open <id>` | Default | Opens the Event Hub (Rewards, Shop, Layout, Spin) |
| `/el spin <id>` | Default | Opens the Event Spin Wheel |
| `/el shop <id>` | Default | Opens the Event Shop |
| `/el claim <id>` | Default | Claims the daily login reward |
| `/el view <id>` | Default | Views the 54-slot showcase layout |
| `/el list` | Default | Lists available events |
| `/el info <id>` | Default | Shows detailed event info |
| `/el create <id>` | `eventslib.admin` | Creates a new event |
| `/el setitem <id> [daily_amount]` | `eventslib.admin` | Sets held item as the event collectible item |
| `/el spincost <id> <cost>` | `eventslib.admin` | Sets the event spin cost in Shards |
| `/el spin add <id> [weight]` | `eventslib.admin` | Adds held item to the spin wheel pool with weight |
| `/el spin remove <id> <reward_id>` | `eventslib.admin` | Removes a reward from the spin pool |
| `/el spin list <id>` | `eventslib.admin` | Lists all spin rewards with weights and % odds |
| `/el shop add <id> <price>` | `eventslib.admin` | Adds held item to the event shop with price |
| `/el shop remove <id> <item_id>` | `eventslib.admin` | Removes an item from the event shop |
| `/el model <id> <string>` | `eventslib.admin` | Sets custom model data string or integer |
| `/el privacy <id> <private/public>` | `eventslib.admin` | Toggles event privacy |
| `/el edit <id>` | `eventslib.admin` | Opens 54-slot drag-and-drop layout editor |
| `/el delete <id>` | `eventslib.admin` | Deletes an event |
| `/el reload` | `eventslib.admin` | Reloads configuration and database |
