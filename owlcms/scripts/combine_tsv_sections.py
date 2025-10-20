#!/usr/bin/env python3
"""
Combine translated TSV sections back into a single CSV file.
Processes all translation_section_XXX.tsv files and merges them in order.
"""

import csv
import os
import re
from pathlib import Path

def combine_tsv_sections(section_dir, output_file):
    """
    Combine all TSV sections back into a single CSV file.
    
    Args:
        section_dir: Directory containing translation_section_XXX.tsv files
        output_file: Output CSV filename
    """
    
    # Find all translated section files (use _translated versions if available, otherwise use originals)
    all_files = os.listdir(section_dir)
    
    # Prefer _translated files, fall back to originals
    section_files = sorted(
        [f for f in all_files if f.startswith('translation_section_') and '_translated' in f and f.endswith('.tsv')],
        key=lambda x: int(re.search(r'_(\d+)_', x).group(1))
    )
    
    # If no _translated files found, use original files
    if not section_files:
        section_files = sorted(
            [f for f in all_files if f.startswith('translation_section_') and '_translated' not in f and f.endswith('.tsv')],
            key=lambda x: int(re.search(r'_(\d+)\.', x).group(1))
        )
    
    if not section_files:
        print(f"ERROR: No section files found in {section_dir}")
        return False
    
    print(f"Found {len(section_files)} sections to combine...")
    
    header = None
    all_rows = []
    
    # Read all sections (skip header from each section, keep only first one)
    for section_file in section_files:
        section_path = os.path.join(section_dir, section_file)
        section_num = int(re.search(r'_(\d+)\.', section_file).group(1))
        
        with open(section_path, 'r', encoding='utf-8') as f:
            reader = csv.reader(f, delimiter='\t')
            file_header = next(reader)  # Read and discard/verify header
            
            # Verify header consistency
            if header is None:
                header = file_header
                print(f"  Section {section_num:3d}: Language header captured")
            elif header != file_header:
                print(f"  WARNING: Header mismatch in {section_file}")
            
            # Collect only data rows (headers already read and discarded)
            rows = list(reader)
            all_rows.extend(rows)
            print(f"  Section {section_num:3d}: {len(rows):3d} data rows (header removed)")
    
    # Write combined CSV with single header (not repeated)
    with open(output_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter=',', quoting=csv.QUOTE_MINIMAL)
        # Write language header once
        writer.writerow(header)
        # Write all data rows from all sections
        writer.writerows(all_rows)
    
    print(f"\n✓ Combined {len(section_files)} sections")
    print(f"✓ Total rows: {len(all_rows)}")
    print(f"✓ Output: {output_file}")
    return True

if __name__ == '__main__':
    section_dir = r'c:\Dev\git\owlcms_v23stable\owlcms_v23master\shared\src\main\resources\i18n\translation_sections'
    output_file = r'c:\Dev\git\owlcms_v23stable\owlcms_v23master\shared\src\main\resources\i18n\translation4_combined.csv'
    
    combine_tsv_sections(section_dir, output_file)
