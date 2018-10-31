# A local proxy for Forex rates

## Forex API key

The API key should be configured via the the environment variable `FOREX_API_KEY`.


## Live interpreter

To showcase the live interpreter, I added a websocket endpoint at the path `/live`. 
After connecting to this endpoint, you will receive live updates every 5 minutes by default if the most recently updated price changes. 

The update duration is configurable via the parameter `cache-ttl` or the environment variable `FOREX_CACHE_TTL_SECONDS`.

For testing purposes, it is advisable to reduce `cache-ttl` to something around 5 seconds to see faster updates.

Ex:

```
$ FOREX_API_KEY=api_key FOREX_TTL_DURATION_SECONDS=5 sbt run
```

Open `http://0.0.0.0:8888from=USD&to=JPY` in your favorite browser to see the rate from `USD` to `JYP` using the get endpoint.

Connect via WebSocket to `http://0.0.0.0:8888/live?from=USD&to=JPY` to see live updates of changes.

## Changes

Remove eff-monad and monix. cats-effect `IO` is more than enough and works out of the box with fs2 for streaming.

## Cache

Results are cached for a default period of 5 minutes which is configurable.

The cache implementation uses `scalacache` backed by `cache2k`. 

## Error handling

More readable error types were added. If the live endpoint fails, the client will receive an error before the websockt connection is closed.

To help identify error messages from normal updates on the client side, all errors will have the following shape:

```json

{
  "type": "Error",
  "code": "MarketClosed" | "QuotaExceeded" | "RateNotFound" | "System",
  "message": "description..."
}


```

## Tests

I added a few test cases.

To run tests:

```
$ FOREX_API_KEY=api_key sbt test
```