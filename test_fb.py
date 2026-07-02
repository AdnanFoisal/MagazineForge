import requests

url = "https://firebasestorage.googleapis.com/v0/b/magazineforge-14d44.appspot.com/o?uploadType=media&name=test.txt"
headers = {"Content-Type": "text/plain"}
data = b"hello world"
resp = requests.post(url, headers=headers, data=data)
print(resp.status_code, resp.text)
