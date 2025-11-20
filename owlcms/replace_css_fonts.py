import os

target_dir = "C:/Dev/git/owlcms-video/competitions/bolivarian/local/css"

print(f"Scanning {target_dir} for font replacements...")

for root, dirs, files in os.walk(target_dir):
    for file in files:
        if file.endswith(".css"):
            file_path = os.path.join(root, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content
            
            # Replace Montserrat with BigTitle
            if "'Montserrat'" in new_content or '"Montserrat"' in new_content:
                print(f"Updating {file}: Replacing Montserrat with BigTitle")
                new_content = new_content.replace("'Montserrat'", "'BigTitle'")
                new_content = new_content.replace('"Montserrat"', "'BigTitle'")
            
            # Check for Roboto Sans (just in case)
            if "'Roboto Sans'" in new_content or '"Roboto Sans"' in new_content:
                print(f"Updating {file}: Replacing Roboto Sans with BigTitle")
                new_content = new_content.replace("'Roboto Sans'", "'BigTitle'")
                new_content = new_content.replace('"Roboto Sans"', "'BigTitle'")

            if new_content != content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)

print("Replacement complete.")
