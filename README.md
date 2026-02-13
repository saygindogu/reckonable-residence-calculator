# Reckonable Residence Calculator

A tool to help estimate reckonable residence days for Irish naturalisation (citizenship) applications under the Irish Nationality and Citizenship Act. It tracks IRP stamp periods, travel absences, and the 70-day annual excuse allowance to estimate when you may be eligible to apply.

> **Disclaimer:** This tool is **not official** and is **not affiliated** with the Department of Justice, the Irish Naturalisation and Immigration Service (INIS), or any Irish government body. It is provided as-is for personal reference only. The calculations may not accurately reflect your legal reckonable residence status. Always consult the [Department of Justice](https://www.irishimmigration.ie/) or a qualified immigration solicitor before making any decisions based on this tool's output. Use at your own risk.

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

A markdown report is generated at `out/{date}_reckonable_residence_output.md` containing:

- IRP entry periods
- Travel history with day counts
- Year-by-year absence breakdown (with the 70-day excuse allowance)
- Summary of accumulated days
- Estimated earliest application date (with and without excuses)
- IRP renewal date

## Notes

- The 1826-day (5 year) reckonable residence goal is based on the standard requirement for naturalisation by residence
- The 70-day annual excuse allowance is based on Irish naturalisation guidelines
- Travel start date is inclusive (counts as a day abroad), return date is exclusive (counts as a day in Ireland)
- Days spent in Ireland before your first IRP are included, but may require additional authorisation from the Department of Justice (e.g. pandemic-related concessions)
- Your personal data files (`irp_info.yaml`, `travels.csv`) are git-ignored to prevent accidental exposure
