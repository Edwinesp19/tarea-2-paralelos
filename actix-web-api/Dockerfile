# Build stage
FROM rust:1.74-slim as builder

# Create a new empty shell project
WORKDIR /usr/src/app
COPY Cargo.toml Cargo.lock ./

# Build only the dependencies to cache them
RUN mkdir src && \
    echo "fn main() {}" > src/main.rs && \
    cargo build --release && \
    rm -rf src

# Now add the actual source code
COPY src ./src

# Build for release
RUN cargo build --release

# Final stage
FROM debian:bullseye-slim

# Install OpenSSL and CA certificates
RUN apt-get update && \
    apt-get install -y openssl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy the binary from builder
COPY --from=builder /usr/src/app/target/release/actix-web-api /usr/local/bin/

# Create a non-root user
RUN useradd -m appuser
USER appuser

# Run the binary
CMD ["actix-web-api"]

