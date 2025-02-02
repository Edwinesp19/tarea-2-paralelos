# Builder stage
FROM --platform=linux/amd64 rust:latest as builder
WORKDIR /usr/src/app
COPY . .
RUN cargo update && cargo build --release

# Runtime stage
FROM --platform=linux/amd64 debian:bookworm-slim
# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    libssl-dev \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/local/bin
# Copy the compiled binary from builder
COPY --from=builder /usr/src/app/target/release/rust_api .
# Copy the environment file if it exists
COPY --from=builder /usr/src/app/.env .

# Expose the port your application listens on
EXPOSE 8080

# Run the binary
CMD ["rust_api"]

