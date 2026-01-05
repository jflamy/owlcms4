#!/usr/bin/env python3
"""
Generate Technical Officials CSV from OWLCMS Database JSON

Extracts officials from session assignments and creates a CSV file
suitable for importing into OWLCMS. Team numbers are assigned based
on session chronological order (first session jury = team 1, etc.)

Usage: python generate_technical_officials_csv.py <database.json> <output.csv>
"""

import json
import sys
import csv
import re
from datetime import datetime
from collections import defaultdict

def normalize_official_name(full_name):
    """
    Normalize official name for comparison - handles inconsistent federation codes.
    Returns: normalized name for grouping comparison (name without federation)
    """
    if not full_name or full_name.strip() == "":
        return ""
    
    # Normalize multiple spaces to single space
    normalized = ' '.join(full_name.strip().split())
    
    parts = normalized.split()
    if len(parts) >= 2:
        # Last part might be the federation code (3 letters)
        last_part = parts[-1]
        if len(last_part) == 3 and last_part.isupper():
            # Return name without federation for comparison
            return " ".join(parts[:-1])
    
    # Return full normalized name
    return normalized

def are_names_similar(name1, name2):
    """
    Heuristic to match names that are likely the same person.
    Returns True if one name is a prefix/substring of the other.
    Example: "QIRMASH ESAM" matches "QIRMASH ESAM ALI"
    """
    if name1 == name2:
        return True
    
    # Check if one is a prefix of the other (handles abbreviated names)
    shorter = name1 if len(name1) < len(name2) else name2
    longer = name2 if len(name1) < len(name2) else name1
    
    # If the shorter name is a prefix of the longer name
    if longer.startswith(shorter + " "):
        return True
    
    return False

def find_matching_grouping(new_grouping, seen_groupings):
    """
    Find if new_grouping matches any existing grouping using name similarity heuristic.
    Returns the matching grouping index or -1 if no match.
    """
    for idx, existing_grouping in enumerate(seen_groupings):
        # Check if groupings have the same size
        if len(new_grouping) != len(existing_grouping):
            continue
        
        # Try to match each name in new_grouping to existing_grouping
        matched = True
        for new_name in new_grouping:
            found_match = False
            for existing_name in existing_grouping:
                if are_names_similar(new_name, existing_name):
                    found_match = True
                    break
            if not found_match:
                matched = False
                break
        
        if matched:
            return idx
    
    return -1

def map_role_for_import(role):
    """
    Map session role to TechnicalOfficial.Role enum for CSV import.
    Most technical roles map to TECHNICAL_OFFICIAL.
    """
    role_mapping = {
        'COMPETITION_DIRECTOR': 'COMPETITION_DIRECTOR',
        'COMPETITION_SECRETARY': 'COMPETITION_SECRETARY',
        'DOCTOR': 'DOCTOR'
    }
    return role_mapping.get(role, 'TECHNICAL_OFFICIAL')

def parse_official_name(full_name):
    """
    Parse official name like 'SOLOVYEVA OLGA KAZ'
    Returns: (full_name_for_lastname, federation_code)
    Normalizes whitespace to handle data inconsistencies
    """
    if not full_name or full_name.strip() == "":
        return ("", "")
    
    # Normalize multiple spaces to single space
    normalized = ' '.join(full_name.strip().split())
    
    parts = normalized.split()
    if len(parts) >= 2:
        # Last part is likely the federation code (3 letters)
        last_part = parts[-1]
        if len(last_part) == 3 and last_part.isupper():
            federation = last_part
            # Everything else is the name (put in LastName field)
            name = " ".join(parts[:-1])
            return (name, federation)
    
    # If no clear federation code, use the whole name
    return (normalized, "")

def map_role_to_detailed(role_field):
    """
    Map session role field names to detailed OfficialRole enum values (for display/tracking)
    """
    role_mapping = {
        'jury1': 'JURY_PRESIDENT',
        'jury2': 'JURY_A',
        'jury3': 'JURY_B',
        'jury4': 'JURY_C',
        'jury5': 'JURY_D',
        'reserveJury': 'JURY_RESERVE',
        'referee1': 'LEFT_REFEREE',
        'referee2': 'CENTER_REFEREE',
        'referee3': 'RIGHT_REFEREE',
        'reserve': 'REFEREE_RESERVE',
        'announcer': 'ANNOUNCER',
        'marshall': 'MARSHAL1',  # Chief Marshal
        'marshal2': 'MARSHAL2',  # Assistant Marshal
        'technicalController': 'TECHNICAL_CONTROLLER1',
        'technicalController2': 'TECHNICAL_CONTROLLER2',
        'technicalController3': 'TECHNICAL_CONTROLLER3',
        'timeKeeper': 'TIMEKEEPER',
        'competitionDirector': 'COMPETITION_DIRECTOR',
        'competitionSecretary': 'COMPETITION_SECRETARY',
        'competitionSecretary2': 'COMPETITION_SECRETARY2',
        'doctor': 'DOCTOR',
        'doctor2': 'DOCTOR2',
        'doctor3': 'DOCTOR3',
        'weighIn1': 'WEIGHIN1',
        'weighIn2': 'WEIGHIN2',
        'tis1': 'TIS1',
        'tis2': 'TIS2'
    }
    return role_mapping.get(role_field, role_field.upper())

def map_role_to_enum(role_field):
    """
    Map session role field names to simplified role categories (for team assignment)
    """
    role_mapping = {
        'jury1': 'JURY',
        'jury2': 'JURY',
        'jury3': 'JURY',
        'jury4': 'JURY',
        'jury5': 'JURY',
        'reserveJury': 'JURY',  # Changed from RESERVE_JURY to JURY
        'referee1': 'REFEREE',
        'referee2': 'REFEREE',
        'referee3': 'REFEREE',
        'reserve': 'REFEREE',  # Changed from RESERVE_REFEREE to REFEREE
        'announcer': 'ANNOUNCER',
        'marshall': 'MARSHAL',
        'marshal2': 'MARSHAL',
        'technicalController': 'TECHNICAL_CONTROLLER',
        'technicalController2': 'TECHNICAL_CONTROLLER',
        'technicalController3': 'TECHNICAL_CONTROLLER',
        'timeKeeper': 'TIMEKEEPER',
        'competitionDirector': 'COMPETITION_DIRECTOR',
        'competitionSecretary': 'COMPETITION_SECRETARY',
        'competitionSecretary2': 'COMPETITION_SECRETARY',
        'doctor': 'DOCTOR',
        'doctor2': 'DOCTOR',
        'doctor3': 'DOCTOR',
        'weighIn1': 'WEIGH_IN',
        'weighIn2': 'WEIGH_IN',
        'tis1': 'TECHNICAL_OFFICIAL',
        'tis2': 'TECHNICAL_OFFICIAL'
    }
    return role_mapping.get(role_field, 'TECHNICAL_OFFICIAL')

def map_to_official_role(role_field):
    """
    Map session role field names to OfficialRole enum values for CSV import.
    Uses generic roles (JURY_MEMBER, REFEREE) for rotating positions - the session
    assignment generator dynamically assigns specific positions during rotation.
    Uses detailed roles for fixed positions (DOCTOR, ANNOUNCER, etc.)
    """
    role_mapping = {
        # Jury roles - use generic JURY_MEMBER (except president)
        'jury1': 'JURY_PRESIDENT',  # President is fixed, not rotating
        'jury2': 'JURY_MEMBER',
        'jury3': 'JURY_MEMBER',
        'jury4': 'JURY_MEMBER',
        'jury5': 'JURY_MEMBER',
        'reserveJury': 'JURY_MEMBER',
        # Referee roles - use generic REFEREE
        'referee1': 'REFEREE',
        'referee2': 'REFEREE',
        'referee3': 'REFEREE',
        'reserve': 'REFEREE',
        # Fixed technical official roles - use detailed values
        'announcer': 'ANNOUNCER',
        'marshall': 'MARSHAL1',
        'marshal2': 'MARSHAL2',
        'technicalController': 'TECHNICAL_CONTROLLER1',
        'technicalController2': 'TECHNICAL_CONTROLLER2',
        'technicalController3': 'TECHNICAL_CONTROLLER2',
        'timeKeeper': 'TIMEKEEPER',
        'weighIn1': 'WEIGHIN1',
        'weighIn2': 'WEIGHIN2',
        # Competition roles - use detailed values
        'competitionDirector': 'COMPETITION_DIRECTOR',
        'competitionSecretary': 'COMPETITION_SECRETARY',
        'competitionSecretary2': 'COMPETITION_SECRETARY2',
        # Medical roles - use detailed values
        'doctor': 'DOCTOR',
        'doctor2': 'DOCTOR2',
        'doctor3': 'DOCTOR3',
        # TIS roles
        'tis1': 'TIS1',
        'tis2': 'TIS2',
    }
    return role_mapping.get(role_field, '')

def parse_competition_time(time_array):
    """
    Parse competition time from JSON array [year, month, day, hour, minute]
    Returns datetime object for sorting
    """
    if not time_array or len(time_array) < 3:
        return datetime.max  # Put sessions without time at the end
    
    year = time_array[0]
    month = time_array[1]
    day = time_array[2]
    hour = time_array[3] if len(time_array) > 3 else 0
    minute = time_array[4] if len(time_array) > 4 else 0
    
    return datetime(year, month, day, hour, minute)

def extract_officials_from_database(database_path):
    """
    Extract all officials from sessions/groups in the database
    Returns: list of (official_name, role, session_time, session_name)
    """
    with open(database_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    groups = data.get('groups', [])
    
    officials_list = []
    
    # Role fields to check in each group
    role_fields = [
        'jury1', 'jury2', 'jury3', 'jury4', 'jury5', 'reserveJury',
        'referee1', 'referee2', 'referee3', 'reserve',
        'announcer', 'marshall', 'marshal2',
        'technicalController', 'technicalController2', 'technicalController3',
        'timeKeeper',
        'competitionDirector', 'competitionSecretary', 'competitionSecretary2',
        'doctor', 'doctor2', 'doctor3',
        'weighIn1', 'weighIn2',
        'tis1', 'tis2'
    ]
    
    for group in groups:
        session_time = parse_competition_time(group.get('competitionTime', []))
        session_name = group.get('name', 'Unknown')
        session_desc = group.get('description', '')
        
        for field in role_fields:
            official_name = group.get(field)
            if official_name and official_name.strip():
                # Normalize whitespace in official names
                normalized_name = ' '.join(official_name.strip().split())
                role = map_role_to_enum(field)
                detailed_role = map_role_to_detailed(field)
                official_role = map_to_official_role(field)
                officials_list.append({
                    'name': normalized_name,
                    'role': role,
                    'detailed_role': detailed_role,
                    'official_role': official_role,
                    'session_time': session_time,
                    'session_name': session_name,
                    'session_desc': session_desc
                })
    
    return officials_list

def assign_team_numbers(officials_list):
    """
    Assign team numbers based on role and session chronological order.
    Each role category gets its own team numbering based on unique groupings.
    """
    # Group officials by role
    from collections import defaultdict
    by_role = defaultdict(list)
    
    for official in officials_list:
        by_role[official['role']].append(official)
    
    # For each role, sort by session time and assign sequential team numbers
    for role, role_officials in by_role.items():
        # Sort by session time
        role_officials.sort(key=lambda x: x['session_time'])
        
        # Group by unique sets of officials per session (not just session time)
        # This handles when the same people work multiple sessions
        session_groupings = []
        seen_groupings = []
        
        for official in role_officials:
            session_time = official['session_time']
            # Get all official names for this session, normalized for comparison
            officials_in_session = tuple(sorted([normalize_official_name(o['name']) for o in role_officials if o['session_time'] == session_time]))
            
            # Check if this grouping matches any existing grouping using similarity heuristic
            matching_idx = find_matching_grouping(officials_in_session, seen_groupings)
            
            if matching_idx == -1:
                # New grouping
                seen_groupings.append(officials_in_session)
                session_groupings.append((session_time, officials_in_session))
        
        # Debug output for JURY and TECHNICAL_CONTROLLER to investigate groupings
        print(f"\n{role}:")
        for idx, (session_time, grouping) in enumerate(session_groupings, start=1):
            # Count how many sessions use this grouping and list them, sorted by time
            sessions_with_grouping = [(o['session_name'], o['session_time']) for o in role_officials 
                                      if tuple(sorted([normalize_official_name(x['name']) for x in role_officials if x['session_time'] == o['session_time']])) == grouping]
            # Remove duplicates and sort by session_time
            unique_sessions = sorted(set(sessions_with_grouping), key=lambda x: x[1])
            session_names = [s[0] for s in unique_sessions]
            print(f"  Team {idx} ({len(grouping)} officials, {len(session_names)} sessions):")
            print(f"    Officials: {', '.join(list(grouping))}")
            print(f"    Sessions: {', '.join(session_names)}")
        
        # Assign sequential team numbers (1, 2, 3, 4) to each unique grouping
        grouping_to_team = {}
        for idx, (session_time, grouping) in enumerate(session_groupings, start=1):
            if idx <= 4:
                grouping_to_team[grouping] = idx
            else:
                grouping_to_team[grouping] = None
        
        # Apply team numbers to all officials based on their session grouping
        for official in role_officials:
            session_time = official['session_time']
            officials_in_session = tuple(sorted([normalize_official_name(o['name']) for o in role_officials if o['session_time'] == session_time]))
            
            # Find matching grouping using similarity heuristic
            matching_idx = find_matching_grouping(officials_in_session, seen_groupings)
            if matching_idx != -1:
                matched_grouping = seen_groupings[matching_idx]
                official['team'] = grouping_to_team[matched_grouping]
            else:
                official['team'] = grouping_to_team.get(officials_in_session)
    
    return officials_list

def map_role_for_import(role):
    """
    Map session role to TechnicalOfficial.Role enum for CSV import.
    Most technical roles map to TECHNICAL_OFFICIAL.
    """
    role_mapping = {
        'COMPETITION_DIRECTOR': 'COMPETITION_DIRECTOR',
        'COMPETITION_SECRETARY': 'COMPETITION_SECRETARY',
        'DOCTOR': 'DOCTOR'
    }
    return role_mapping.get(role, 'TECHNICAL_OFFICIAL')

def generate_csv(officials_list, output_path):
    """
    Generate CSV file with correct columns for OWLCMS import
    """
    # Deduplicate officials by name - keep first occurrence (earliest session) with lowest team number
    unique_officials = {}
    
    for official in officials_list:
        name = official['name']
        if name not in unique_officials:
            unique_officials[name] = official
        else:
            # If official appears in multiple sessions, keep one with lowest team number
            existing = unique_officials[name]
            if official['team'] and existing['team']:
                if official['team'] < existing['team']:
                    unique_officials[name] = official
            elif official['team'] and not existing['team']:
                unique_officials[name] = official
    
    # CSV columns matching XLSXTechnicalOfficialsExport order + OfficialRole
    headers = ['Active', 'Role', 'LastName', 'FirstName', 'Level', 
               'FederationId', 'Federation', 'Affiliation', 'IWFId', 'Team', 'OfficialRole']
    
    with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=headers)
        writer.writeheader()
        
        # Sort by role then by team, then by last name
        sorted_officials = sorted(unique_officials.values(), 
                                  key=lambda x: (x['role'], x['team'] or 999, parse_official_name(x['name'])[0]))
        
        for official in sorted_officials:
            last_name, federation = parse_official_name(official['name'])
            
            row = {
                'Active': 'true',  # Mark all as active
                'Role': map_role_for_import(official['role']),
                'LastName': last_name,
                'FirstName': '',  # Empty as per user request
                'Level': '',  # Not available from session data
                'FederationId': '',
                'Federation': federation,
                'Affiliation': '',
                'IWFId': '',
                'Team': official['team'] if official['team'] else '',
                'OfficialRole': official.get('official_role', '')
            }
            writer.writerow(row)
    
    print(f"Generated {len(unique_officials)} officials in {output_path}")
    
    # Print team distribution summary by role
    from collections import defaultdict
    role_team_counts = defaultdict(lambda: defaultdict(int))
    for official in unique_officials.values():
        role = official['role']
        team = official['team'] if official['team'] else 'None'
        role_team_counts[role][team] += 1
    
    print("\nTeam distribution by role:")
    for role in sorted(role_team_counts.keys()):
        print(f"\n  {role}:")
        for team in sorted(role_team_counts[role].keys(), key=lambda x: (x == 'None', x if isinstance(x, int) else 999)):
            print(f"    Team {team}: {role_team_counts[role][team]} officials")

def generate_timetable(officials_list, output_path):
    """
    Generate timetable CSV as a grid with sessions as rows and roles as columns
    """
    # Canonical role order
    canonical_roles = ['JURY', 'REFEREE', 'MARSHAL', 'TIMEKEEPER', 
                      'TECHNICAL_CONTROLLER', 'COMPETITION_SECRETARY', 'DOCTOR']
    
    # Build session -> role -> team mapping
    session_role_team = defaultdict(dict)
    all_sessions = set()
    
    for official in officials_list:
        role = official['role']
        team = official['team']
        session = official['session_name']
        if team and role in canonical_roles:
            session_role_team[session][role] = team
            all_sessions.add(session)
    
    # Sort sessions numerically
    sorted_sessions = sorted(all_sessions, key=lambda x: int(x) if x.isdigit() else x)
    
    # Write timetable CSV as grid
    with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.writer(csvfile)
        
        # Header row
        writer.writerow(['Session'] + canonical_roles)
        
        # Data rows
        for session in sorted_sessions:
            row = [session]
            for role in canonical_roles:
                team = session_role_team[session].get(role, '')
                row.append(team)
            writer.writerow(row)
    
    print(f"Generated timetable: {output_path}")

def generate_detailed_roles_csv(officials_list, output_path):
    """
    Generate CSV with detailed roles (JURY_PRESIDENT, CHIEF_MARSHAL, etc.) showing
    name, detailed role, team, and sessions
    """
    # Deduplicate by name and detailed_role
    unique_assignments = {}
    
    for official in officials_list:
        key = (official['name'], official['detailed_role'])
        if key not in unique_assignments:
            unique_assignments[key] = {
                'name': official['name'],
                'detailed_role': official['detailed_role'],
                'team': official['team'],
                'sessions': []
            }
        unique_assignments[key]['sessions'].append(official['session_name'])
    
    # Write detailed roles CSV
    with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(['Name', 'Detailed Role', 'Team', 'Sessions'])
        
        # Sort by detailed role then by team then by name
        sorted_assignments = sorted(unique_assignments.values(), 
                                   key=lambda x: (x['detailed_role'], x['team'] or 999, x['name']))
        
        for assignment in sorted_assignments:
            sessions = sorted(set(assignment['sessions']), key=lambda x: int(x) if x.isdigit() else x)
            writer.writerow([assignment['name'], assignment['detailed_role'], 
                           assignment['team'], ', '.join(sessions)])
    
    print(f"Generated detailed roles: {output_path}")

def main():
    if len(sys.argv) != 2:
        print("Usage: python generate_technical_officials_csv.py <database.json>")
        print("\nExample:")
        print("  python generate_technical_officials_csv.py ISGDatabase_2026-01-01_13h24.json")
        print("\nOutputs:")
        print("  - to_import.csv: Officials list for OWLCMS import")
        print("  - timetable.csv: Role/team assignments by session")
        print("  - detailed_roles.csv: Detailed role assignments (JURY_PRESIDENT, CHIEF_MARSHAL, etc.)")
        sys.exit(1)
    
    database_path = sys.argv[1]
    
    print(f"Reading database from: {database_path}")
    officials_list = extract_officials_from_database(database_path)
    print(f"Found {len(officials_list)} official assignments across all sessions")
    
    print("\nAssigning team numbers by role based on session chronological order...")
    officials_list = assign_team_numbers(officials_list)
    
    print("\nGenerating to_import.csv")
    generate_csv(officials_list, 'to_import.csv')
    
    print("Generating timetable.csv")
    generate_timetable(officials_list, 'timetable.csv')
    
    print("Generating detailed_roles.csv")
    generate_detailed_roles_csv(officials_list, 'detailed_roles.csv')
    
    print("\n✓ Done! Import to_import.csv into OWLCMS via Prepare Competition → Technical Officials → Upload")

if __name__ == '__main__':
    main()
