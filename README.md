# valr-matching-engine

This is an implementation of a high-performance matching engine for a trading platform.

## Overview

The `valr-matching-engine` has the following components:

1. An `HttpServer` Verticle built using the `vert.x HttpServer`.
2. An `OrderService` which validates requests to the `HttpServer` and sends them on to the `Orderbook` using a `vert.x EventBus`.
3. An `OrderValidationService` which contains the validation logic for all requests.
4. An abstract `BaseOrderBook` Verticle which is implemented by:
   1. The `OrderBook` class which is my implementation of a high performance orderbook.
   2. The `SimpleOrderBook` class which is a naive implementation used for testing and to check correctness of the `OrderBook` implementation.

## Design

The `valr-matching-engine` tries to leverage `vert.x` toolkit to make the engine performant and scalable.
As such, the `HttpServer` and `BaseOrderBook` are both Verticles. In a production environment I would run the engine as follows:

1. Deploy multiple instances of the `HttpServer` and load balance across them.
2. For each `pair`, deploy a cluster of `OrderBook` instances managed by a consensus algorithm so that there is one leader which can perform work. For example: in a `Hazelcast cluster`.
3. Manage communication between the `HttpServer` instances and the `OrderBook` instances using an `EventBus` also managed by `Hazlecast`.

### OrderBook

The `OrderBook` class is implemented based on the principles outlined in the article: [How to Build a Fast Limit Order Book](https://web.archive.org/web/20110219163448/http://howtohft.wordpress.com/2011/02/15/how-to-build-a-fast-limit-order-book/).

As such, the `OrderBook` has:
   1. Two self-balancing (Red Black) Binary Search  trees for `bids` and `asks`.
   2. These Binary Search trees contain `Level` objects which represent an orderbook level at a certain price.
      * Each `Level` contains a `price`, `totalAmount` representing the volume at the level and a doubly linked list of `LimitOrder`s at that level.
   3. A `MutableMap` of `price` to `Node<Level>`s.
   4. A `MutableMap` of `orderId` to `LimiOrder`s.
   5. A `BestBid: Node<Level>?` and `BestAsk: Node<Level>?`

Taking `N` as the number of `LimitOrder`s and `M` as the number of `Level`s, this allows these operations to have the following time complexity:
- **Add Order**:
    - First limit order at a price: `O(log M)` (`M ≤ N` but reasonably `M < N`)
    - Subsequent limit orders at the same price: `O(1)`
- **Cancel Order**: `O(1)`
- **Execute Order**: `O(1)`

## Running the Application

### Running with Docker

To run the `valr-matching-engine` in a Docker container, follow these steps:

1. **Build the Docker image**:
    ```bash
    docker build . -t valr-matching-engine
    ```

2. **Run the Docker container**:
    ```bash
    docker run -p 8080:8080 valr-matching-engine
    ```

This will launch the matching engine, including the HTTP server and order books, inside a Docker container and host machines port `8080` to the containers port `8080`.

### Running with Gradle

To run the project directly with Gradle (without Docker), use the following command:

```bash
./gradlew run
```

And to run the tests, use the following command:

```bash
./gradlew test
```

## Notes & Further Considerations

Firstly, I am new to `Kotlin` and as such I am not necessarily aware of best practices or some performance considerations. 
I am pretty sure everything works but there may be a few oddities. If there are I am really keen to hear about them.

On further improvements:

1. Be more efficient with object creation (and potential reuse) to try and reduce time spent garbage collecting.
    * For example the `MarketOrder`s, `LimitOrder`s, and `Level`s could be managed by corresponding `Object Pool`s (although I haven't done something like this before).
2. Actually implement the code to use `Hazelcast` so that the Verticles can be deployed on an architecture optimized for performance.
3. Implement authentication for the placement and cancellation of orders.
4. Track orders submitted by a user so that their status can be queried. 
5. Analyse how the orderbook is used in a production environment and tweak implementation for efficiency. For example if there is better knowledge 
about the pattern of order placements you could edit the data structures used to accommodate this.

## Interacting with the Application

You can interact with the application via the following HTTP endpoints. Below are the example `curl` requests for each endpoint.

### 1. Place a Market Order

**POST** `/orders/market`

#### Example Request

```bash
curl -X POST http://localhost:8080/orders/market \
     -H "Content-Type: application/json" \
     -d '{
           "side": "BUY",
           "pair": "BTCUSD",
           "quoteAmount": 70000.0
         }'
```

### 2. Place a Limit Order

*POST* `/orders/limit`

#### Example Request

```bash
curl -X POST http://localhost:8080/orders/limit \
     -H "Content-Type: application/json" \
     -d '{
           "side": "SELL",
           "pair": "BTCUSD",
           "quantity": 1.0,
           "price": 70000.0
         }'
```

### 3. Cancel an Order

*DELETE* `/orders/order`

#### Example Request

```bash
curl -X DELETE http://localhost:8080/orders/order \
     -H "Content-Type: application/json" \
     -d '{
           "orderId": "7cd0c1be-62eb-487a-94c5-94bc23d5e718",
           "pair": "BTCUSD"
         }'
```

### 4. Get the Order Book for a Currency Pair

*GET* `/public/:currencyPair/orderbook`

#### Example Request

```bash
curl -X GET http://localhost:8080/public/BTCUSD/orderbook
```

#### Example Response

```json
{
  "Bids": [
    {
      "side": "BUY",
      "quantity": 1,
      "price": 1,
      "pair": "BTCUSD",
      "timestamp": 1730974455472
    }
  ],
  "Asks": []
}
```

### 4. Get the Order Book for a Currency Pair

*GET* `/public/:currencyPair/orderbook`

#### Example Request

```bash
curl -X GET http://localhost:8080/public/BTCUSD/orderbook
```

#### Example Response

```json
{
  "Bids": [
    {
      "side": "BUY",
      "quantity": 1,
      "price": 70000,
      "pair": "BTCUSD",
      "timestamp": 1730974455472
    }
  ],
  "Asks": []
}
```

### 5. Get Trade History for a Currency Pair

*GET* `/marketdata/:currencyPair/tradehistory`

#### Example Request

```bash
curl -X GET "http://localhost:8080/marketdata/BTCUSD/tradehistory?offset=0&limit=10"
```

#### Example Response

```json
[
  {
    "id": "d54c18ce-f9e6-4439-ac10-57f7d9a95422",
    "price": 70000,
    "quantity": 0.5,
    "currencyPair": "BTCUSD",
    "tradedAt": 1730978979573,
    "takerSide": "SELL",
    "sequenceId": 0,
    "quoteVolume": 0.5
  }
]
```
