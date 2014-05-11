import pyjsonrpc
rpc_client = pyjsonrpc.HttpClient(url = "http://127.0.0.1:54321/chancecoin")

print(rpc_client)
print(rpc_client.getChancecoinBalance("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM"))
