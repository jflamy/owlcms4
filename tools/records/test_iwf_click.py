#!/usr/bin/env python3
"""Test script to verify the IWF website interaction works."""

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC
import time

# Setup Chrome driver (not headless so we can see what happens)
options = webdriver.ChromeOptions()
# options.add_argument('--headless')  # Comment out to see the browser

driver = webdriver.Chrome(options=options)

try:
    print("Loading page...")
    driver.get("https://iwf.sport/results/world-records/")
    
    # Wait for page to load
    wait = WebDriverWait(driver, 15)
    
    print("Finding dropdowns...")
    # First dropdown: Record type (we want "Current")
    record_type_select = wait.until(
        EC.presence_of_element_located((By.ID, "ranking_curprog"))
    )
    record_type = Select(record_type_select)
    print(f"Record type options: {[opt.text for opt in record_type.options]}")
    record_type.select_by_visible_text("Current")
    print("Selected: Current")
    
    # Second dropdown: Age group
    age_group_select = driver.find_element(By.NAME, "ranking_agegroup")
    age_group = Select(age_group_select)
    age_group_options = [opt.text for opt in age_group.options if opt.text.strip()]
    print(f"Age group options: {age_group_options}")
    
    # Third dropdown: Gender
    gender_select = driver.find_element(By.ID, "ranking_gender")
    gender = Select(gender_select)
    gender_options = [opt.text for opt in gender.options if opt.text.strip()]
    print(f"Gender options: {gender_options}")
    
    # Select Junior and Women
    print("\nSelecting Junior and Women...")
    age_group.select_by_visible_text("Junior")
    gender_select = driver.find_element(By.ID, "ranking_gender")
    gender = Select(gender_select)
    gender.select_by_visible_text("Women")
    
    print("Clicking search button via JavaScript...")
    driver.execute_script("document.querySelector('input[type=\"submit\"][value=\"Search\"]').click();")
    
    print("Waiting 2 seconds for results to load...")
    time.sleep(2)
    
    # Save the HTML to a file for inspection
    print("\nSaving page HTML...")
    with open("iwf_page.html", "w", encoding="utf-8") as f:
        f.write(driver.page_source)
    print("Saved to iwf_page.html")
    
    # Try to find the results table
    print("\nLooking for results table...")
    tables = driver.find_elements(By.TAG_NAME, "table")
    print(f"Found {len(tables)} tables")
    
    # Try different selectors
    print("\nTrying CSS selectors...")
    results_divs = driver.find_elements(By.CSS_SELECTOR, ".ranking-results")
    print(f"Found {len(results_divs)} .ranking-results divs")
    
    results_tables = driver.find_elements(By.CSS_SELECTOR, "table.records-table")
    print(f"Found {len(results_tables)} table.records-table elements")
    
    all_divs = driver.find_elements(By.TAG_NAME, "div")
    print(f"Total divs on page: {len(all_divs)}")
    
    # Print page source snippet to see structure
    print("\nPage source around 'World Record' (first 500 chars):")
    source = driver.page_source
    if "World Record" in source:
        idx = source.index("World Record")
        print(source[max(0, idx-200):idx+300])
    else:
        print("'World Record' not found in source")
    
    for i, table in enumerate(tables):
        print(f"\nTable {i}:")
        print(f"  Class: {table.get_attribute('class')}")
        rows = table.find_elements(By.TAG_NAME, "tr")
        print(f"  Rows: {len(rows)}")
        if len(rows) > 0:
            print(f"  First row text: {rows[0].text[:100]}")
        if len(rows) > 1:
            print(f"  Second row text: {rows[1].text[:100]}")
    
    print("\n\nClosing browser in 3 seconds...")
    time.sleep(3)

finally:
    driver.quit()
    print("Done!")
