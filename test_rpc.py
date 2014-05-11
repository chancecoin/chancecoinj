import pyjsonrpc
rpc_client = pyjsonrpc.HttpClient(url = "http://127.0.0.1:54121/chancecoin", username = "chancecoin", password = "password")

print(rpc_client.getBalance("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM"))
