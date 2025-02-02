FROM --platform=linux/amd64 rust:latest as builder

WORKDIR /usr/src/app
COPY . .

# Update dependencies and build
RUN cargo update && cargo build --release

FROM --platform=linux/amd64 debian:bookworm-slim
RUN apt-get update && apt-get install -y libssl-dev ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/local/bin
COPY --from=builder /usr/src/app/target/release/actix_web_api .
COPY --from=builder /usr/src/app/.env .

EXPOSE 8081
CMD ["./actix_web_api"]
