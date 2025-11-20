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
            
            # Replace CocogoosePro with BigTitle
            if "'CocogoosePro'" in new_content or '"CocogoosePro"' in new_content:
                print(f"Updating {file}: Replacing CocogoosePro with BigTitle")
                new_content = new_content.replace("'CocogoosePro'", "'BigTitle'")
                new_content = new_content.replace('"CocogoosePro"', "'BigTitle'")

            if new_content != content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)

print("Replacement complete.")
