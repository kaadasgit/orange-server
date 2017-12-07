#!/bin/bash

curl -XPOST \
    -d '{"test","qos 0"}' \
    -H "tenant: tenant.local" \
    "http://localhost:2883/pubsub/publish?channel=/tenant.local/channel/1"

curl -XPOST \
    -d '{"test","qos 1"}' \
    -H "tenant: tenant.local" \
    "http://localhost:2883/pubsub/publish?topic=/tenant.local/channel/1&qos=LEAST_ONE"

curl -XPOST \
    -d '{"test","qos 2"}' \
    -H "tenant: tenant.local" \
    "http://localhost:2883/pubsub/publish?topic=/tenant.local/channel/1&qos=EXACTLY_ONCE"
