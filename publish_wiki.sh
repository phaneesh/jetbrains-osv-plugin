#!/bin/bash

# Script to publish documentation to GitHub Wiki

WIKI_REPO="https://github.com/phaneesh/jetbrains-osv-plugin.wiki.git"
LOCAL_WIKI_DIR="/tmp/jetbrains-osv-plugin-wiki"

echo "Publishing OSV Plugin Documentation to GitHub Wiki..."

# Clone the wiki repository
if [ -d "$LOCAL_WIKI_DIR" ]; then
    echo "Wiki directory exists, fetching latest changes..."
    cd "$LOCAL_WIKI_DIR"
    git pull origin master
else
    echo "Cloning wiki repository..."
    git clone "$WIKI_REPO" "$LOCAL_WIKI_DIR"
    cd "$LOCAL_WIKI_DIR"
fi

# Copy documentation files
echo "Copying documentation files..."
cp ../GETTING_STARTED.md Home.md
cp ../PLUGIN_DOCUMENTATION.md Plugin-Documentation.md

# Add all files
git add .
git commit -m "Update plugin documentation"
git push origin master

echo "Documentation published successfully!"
echo "Visit: https://github.com/phaneesh/jetbrains-osv-plugin/wiki"
