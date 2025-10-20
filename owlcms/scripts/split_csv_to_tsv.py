#!/usr/bin/env python3
"""
Split translation4.csv into numbered 20-line TSV sections.
Each section will be saved as translation_section_XXX.tsv

WORKFLOW:
1. Run this script to split the CSV into TSV sections
2. For each section, create a _translated version (e.g., translation_section_001_translated.tsv)
3. Edit the _translated versions with proper zh-TW translations
4. Run combine_tsv_sections.py which will use _translated files if available, otherwise originals
5. The combine script will merge all _translated versions back into a single CSV
"""

import csv
import os
from pathlib import Path

def split_csv_to_tsv_sections(csv_file, lines_per_section=20, output_dir=None):
    """
    Split CSV file into TSV sections.
    
    Args:
        csv_file: Path to input CSV file
        lines_per_section: Number of lines per output file (default 20)
        output_dir: Output directory (default: same as csv_file)
    """
    
    if output_dir is None:
        output_dir = os.path.dirname(csv_file)
    
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    section_num = 1
    line_buffer = []
    total_lines = 0
    
    # Read CSV and collect header
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        header = next(reader)  # Get header row
        
        # Process remaining rows
        for row_num, row in enumerate(reader, start=2):
            # Add this row to buffer
            line_buffer.append(row)
            
            # When buffer reaches desired size, write section
            if len(line_buffer) >= lines_per_section:
                write_section(output_dir, section_num, header, line_buffer[:lines_per_section])
                total_lines += len(line_buffer[:lines_per_section])
                line_buffer = line_buffer[lines_per_section:]
                section_num += 1
        
        # Write remaining lines as final section
        if line_buffer:
            write_section(output_dir, section_num, header, line_buffer)
            total_lines += len(line_buffer)
    
    print(f"✓ Split {csv_file}")
    print(f"✓ Created {section_num} sections with {lines_per_section} lines each (last section may vary)")
    print(f"✓ Total data rows: {total_lines}")
    print(f"✓ Output directory: {output_dir}")
    print(f"\nTo reassemble: python combine_tsv_sections.py {output_dir}")

def write_section(output_dir, section_num, header, rows):
    """Write a section as TSV file with language header."""
    section_filename = os.path.join(output_dir, f"translation_section_{section_num:03d}.tsv")
    
    with open(section_filename, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter='\t', quoting=csv.QUOTE_MINIMAL)
        # Write language header row (with tab delimiters)
        writer.writerow(header)
        # Write data rows
        writer.writerows(rows)
    
    print(f"  Section {section_num:3d}: {len(rows):3d} rows → {Path(section_filename).name}")

if __name__ == '__main__':
    csv_file = r'c:\Dev\git\owlcms_v23stable\owlcms_v23master\shared\src\main\resources\i18n\translation4.csv'
    output_dir = r'c:\Dev\git\owlcms_v23stable\owlcms_v23master\shared\src\main\resources\i18n\translation_sections'
    
    split_csv_to_tsv_sections(csv_file, lines_per_section=20, output_dir=output_dir)
