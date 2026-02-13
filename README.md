# Reckonable Residence Calculator

Calculates reckonable residence days for Irish naturalisation applications. Tracks IRP stamp periods, travel absences, and the 70-day annual excuse allowance to estimate when you can apply.

## Prerequisites

- Java 17+
- Gradle (included via wrapper)

## Setup

1. Copy the example files and fill in your own data:

```sh
cp example_irp_info.yaml irp_info.yaml
cp example_travels.csv travels.csv
```

2. Edit `irp_info.yaml` with your IRP details:

```yaml
ireland_first_entry: 2020-01-15
irp:
  - name: stamp1
    start: 2020-07-01
    end: 2021-07-01
  - name: stamp4
    start: 2021-06-25
    end: 2025-06-25
```

- `ireland_first_entry` - the date you first entered Ireland
- `irp` - list of your IRP stamp periods, each with a name, start date, and end date
- All dates use `yyyy-MM-dd` format

3. Edit `travels.csv` with your travel history:

```csv
start,end,location
2020-08-10,2020-08-25,TR
2021-03-15,2021-03-22,UK
```

- `start` - date you left Ireland
- `end` - date you returned to Ireland
- `location` - country/region visited

## Run

```sh
./gradlew run
```

## Output

The calculator prints:

- Travel history with day counts
- Year-by-year absence breakdown (with the 70-day excuse allowance)
- Total days accumulated
- Estimated application date (with and without excuses)
- IRP renewal date

## Notes

- The 1826-day (5 year) reckonable residence goal is hardcoded
- The 70-day annual excuse allowance is based on Irish naturalisation guidelines
- Travel start date is inclusive (counts as a day abroad), return date is exclusive (counts as a day in Ireland)
- Days spent in Ireland before your first IRP are included, but may require additional authorisation from the Department of Justice
