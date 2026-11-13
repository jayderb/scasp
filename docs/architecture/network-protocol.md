# TCP gateway protocol
Transport: TCP, default port 5050. Encoding: UTF-8, one newline-terminated command per connection.

| Command | Meaning | Example response |
|---|---|---|
| PING | Health check | OK PONG |
| COURSES | Return demo course codes | OK CSC301,DB302,NET303 |
| HELP | List commands | OK PING\|COURSES\|HELP |
| ANNOUNCEMENT | Simulate announcement event | OK Announcement service received |

Malformed or unknown commands receive an ERROR response. The server uses a worker pool, socket timeout and try-with-resources cleanup. Production deployment should add authentication, TLS and structured JSON messages.
