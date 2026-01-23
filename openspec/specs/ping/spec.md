# Ping Module

Query player latency and display ping leaderboard.

## Requirements

### Requirement: Ping Query

Players SHALL be able to check their own or others' ping.

#### Scenario: Check own ping

- **WHEN** player executes `/tsl ping` with permission `tsl.ping.use`
- **THEN** player's current ping is displayed with color coding

#### Scenario: Check other player ping

- **WHEN** player executes `/tsl ping <player>`
- **THEN** target player's ping is displayed

### Requirement: Ping Leaderboard

Operators SHALL be able to view all players' ping sorted.

#### Scenario: View all pings

- **WHEN** player executes `/tsl ping all` with permission `tsl.ping.all`
- **THEN** paginated list of all players sorted by ping is shown

#### Scenario: Pagination

- **WHEN** player executes `/tsl ping all <page>`
- **THEN** specified page of the leaderboard is shown

### Requirement: Ping Color Coding

Ping values SHALL be color-coded based on thresholds.

#### Scenario: Color display

- **WHEN** ping is displayed
- **THEN** green color for ping < green threshold
- **THEN** yellow color for ping < yellow threshold
- **THEN** red color for ping >= yellow threshold

## Configuration

```yaml
ping:
  enabled: false
  entries_per_page: 10
  ping_colors:
    green: 100
    yellow: 200
```
