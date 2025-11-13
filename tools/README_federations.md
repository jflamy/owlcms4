# Federation Checker

Tools for validating continental weightlifting federation membership in competition registration files.

## Files

- `check_federations.py` - Main validation script
- `federations/` - Official membership lists (IOC codes)
  - `EWF_members.csv` - European Weightlifting Federation
  - `AWF_members.csv` - Asian Weightlifting Federation
  - `WFA_members.csv` - Weightlifting Federation of Africa
  - `PAWF_members.csv` - Pan American Weightlifting Federation
  - `OWF_members.csv` - Oceania Weightlifting Federation

## Usage

### Check for federation errors
```bash
python tools/check_federations.py path/to/registration.xlsx
```

### Generate corrected Excel file
```bash
python tools/check_federations.py path/to/registration.xlsx --fix output_FIXED.xlsx
```

### Generate annotated error report
```bash
python tools/check_federations.py path/to/registration.xlsx --annotate report.xlsx
```

## Requirements

```bash
pip install pandas openpyxl
```

## How it works

The script validates that each athlete's continental federation (in column P) matches their country's official membership (from column E - IOC code). It:

1. Reads row 9+ from the first Excel sheet
2. Checks column E (IOC code) against official membership lists
3. Validates column P contains the correct continental federation(s)
4. Reports missing or incorrect federations
5. Can automatically generate corrected files

## Example Output

```
Excel row  41: IOC=KAZ | Found: EWF,IWF | Should be: AWF,IWF | MISSING_AWF | WRONG_EWF
```

This indicates Kazakhstan should be in AWF (Asian) not EWF (European).
