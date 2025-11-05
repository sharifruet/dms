#!/usr/bin/env python3
"""
Script to trigger OCR reprocessing for all existing documents
"""
import requests
import json
import sys

BASE_URL = "http://localhost:8080"

def login(username="admin", password="admin123"):
    """Login and get JWT token"""
    url = f"{BASE_URL}/api/auth/login"
    payload = {"username": username, "password": password}
    
    try:
        response = requests.post(url, json=payload, headers={"Content-Type": "application/json"})
        
        if response.status_code == 200:
            data = response.json()
            return data.get("token")
        else:
            print(f"❌ Login failed: {response.status_code} - {response.text}")
            print("\n💡 Try different passwords:")
            print("   - password")
            print("   - admin123")
            print("   - admin")
            return None
    except Exception as e:
        print(f"❌ Error during login: {e}")
        return None

def reprocess_all_ocr(token):
    """Trigger OCR reprocessing for all documents"""
    url = f"{BASE_URL}/api/documents/reprocess-ocr/all"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    try:
        response = requests.post(url, headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ {data.get('message', 'OCR reprocessing started!')}")
            return True
        else:
            print(f"❌ Failed to trigger OCR: {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error triggering OCR: {e}")
        return False

def reprocess_single_ocr(token, document_id):
    """Trigger OCR reprocessing for a single document"""
    url = f"{BASE_URL}/api/documents/{document_id}/reprocess-ocr"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    try:
        response = requests.post(url, headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ {data.get('message', f'OCR reprocessing started for document {document_id}!')}")
            return True
        else:
            print(f"❌ Failed to trigger OCR: {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error triggering OCR: {e}")
        return False

if __name__ == "__main__":
    print("🔐 Logging in as admin...")
    
    # Try different passwords
    passwords = ["admin123", "password", "admin"]
    token = None
    
    for pwd in passwords:
        token = login("admin", pwd)
        if token:
            break
    
    if not token:
        print("\n❌ Could not login. Please check your credentials.")
        print("💡 You can also trigger OCR reprocessing from the frontend.")
        sys.exit(1)
    
    print("✅ Login successful!")
    print()
    
    # Check if specific document ID provided
    if len(sys.argv) > 1:
        doc_id = sys.argv[1]
        print(f"🔄 Triggering OCR reprocessing for document {doc_id}...")
        reprocess_single_ocr(token, doc_id)
    else:
        print("🔄 Triggering OCR reprocessing for all documents...")
        reprocess_all_ocr(token)
    
    print()
    print("📝 Processing runs asynchronously. Check the backend logs for progress.")
    print("⏱️  OCR processing typically takes 5-60 seconds per document depending on size.")
    print("💡 You can view OCR text in the Document Viewer after processing completes.")

