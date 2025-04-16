
client = new PalletClient("https://pallet.1gb.ru/");
client.login("spec", "kudo");
client.in
    .file("order_data", "input\\order_data.csv")
    .file("warehouse_data", "input\\warehouse_data.csv")
    .file("warehouse_area", "input\\warehouse_area.csv");
client.out
    .file("pallets", "out\\pallets.json");
client.get("/api/work");

