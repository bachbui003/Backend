-- Table: users (Lưu thông tin người dùng, bao gồm cả admin và khách hàng)
CREATE TABLE users (
                       id bigint NOT NULL,
                       username varchar(50) NOT NULL, -- Rút gọn từ character varying
                       password varchar(255) NOT NULL,
                       email varchar(100) NOT NULL,
                       phone varchar(15), -- Giới hạn độ dài hợp lý cho số điện thoại
                       address varchar(255),
                       role varchar(20), -- ADMIN, USER, v.v.
                       full_name varchar(100),
                       created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                       updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id),
    ADD CONSTRAINT users_username_key UNIQUE (username),
    ADD CONSTRAINT users_email_key UNIQUE (email);

-- Table: admin (Thông tin bổ sung cho admin, liên kết với users)
CREATE TABLE admin (
                       id bigint NOT NULL,
                       user_id bigint NOT NULL, -- Liên kết 1-1 với users
                       phone varchar(15),
                       address varchar(255),
                       created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                       updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE admin
    ADD CONSTRAINT admin_pkey PRIMARY KEY (id),
    ADD CONSTRAINT admin_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT admin_user_id_key UNIQUE (user_id);

-- Table: categories (Danh mục sản phẩm, hỗ trợ danh mục cha-con)
CREATE TABLE categories (
                            id bigint NOT NULL,
                            name varchar(255) NOT NULL,
                            parent_id bigint, -- Self-referencing để hỗ trợ danh mục con
                            icon varchar(255),
                            created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                            updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id),
    ADD CONSTRAINT categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Table: products (Sản phẩm chính)
CREATE TABLE products (
                          id bigint NOT NULL,
                          name varchar(255) NOT NULL,
                          description text, -- Đổi sang text để mô tả dài hơn nếu cần
                          price numeric(15,2) NOT NULL, -- Chuẩn hóa thành numeric thay vì double
                          category_id bigint,
                          image_url varchar(500),
                          stock_quantity integer DEFAULT 0,
                          rating numeric(3,2) NOT NULL CHECK (rating >= 0 AND rating <= 5), -- Rating từ 0-5, 2 chữ số thập phân
                          created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                          updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id),
    ADD CONSTRAINT products_category_id_fkey FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Table: variants (Biến thể sản phẩm: màu sắc, kích thước, v.v.)
CREATE TABLE variants (
                          id bigint NOT NULL,
                          product_id bigint,
                          type varchar(50), -- Ví dụ: "color", "size"
                          color varchar(50),
                          price numeric(15,2) NOT NULL,
                          image_url varchar(500),
                          stock_quantity integer DEFAULT 0,
                          parent_variant_id bigint, -- Hỗ trợ biến thể con
                          created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                          updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE variants
    ADD CONSTRAINT variants_pkey PRIMARY KEY (id),
    ADD CONSTRAINT variants_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    ADD CONSTRAINT variants_parent_variant_id_fkey FOREIGN KEY (parent_variant_id) REFERENCES variants(id) ON DELETE SET NULL;

-- Table: carts (Giỏ hàng của người dùng)
CREATE TABLE carts (
                       id bigint NOT NULL,
                       user_id bigint,
                       total_price numeric(15,2) DEFAULT 0,
                       created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                       updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id),
    ADD CONSTRAINT carts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Table: cart_items (Mặt hàng trong giỏ)
CREATE TABLE cart_items (
                            id bigint NOT NULL,
                            cart_id bigint,
                            product_id bigint,
                            quantity integer NOT NULL CHECK (quantity > 0),
                            price numeric(15,2) NOT NULL,
                            created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id),
    ADD CONSTRAINT cart_items_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    ADD CONSTRAINT cart_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

-- Table: orders (Đơn hàng)
CREATE TABLE orders (
                        id bigint NOT NULL,
                        user_id bigint,
                        total_price numeric(15,2) NOT NULL,
                        status varchar(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
                        order_date timestamp DEFAULT CURRENT_TIMESTAMP,
                        payment_method varchar(50),
                        payment_url varchar(500),
                        created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                        updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id),
    ADD CONSTRAINT orders_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Table: order_items (Chi tiết đơn hàng)
CREATE TABLE order_items (
                             id bigint NOT NULL,
                             order_id bigint,
                             product_id bigint,
                             quantity integer NOT NULL CHECK (quantity > 0),
                             price numeric(15,2) NOT NULL,
                             created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id),
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    ADD CONSTRAINT order_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

-- Table: payments (Thanh toán)
CREATE TABLE payments (
                          id bigint NOT NULL,
                          order_id bigint,
                          user_id bigint,
                          amount numeric(15,2) NOT NULL,
                          payment_status varchar(20), -- PENDING, SUCCESS, FAILED
                          transaction_id varchar(50),
                          payment_date timestamp DEFAULT CURRENT_TIMESTAMP,
                          created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id),
    ADD CONSTRAINT payments_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    ADD CONSTRAINT payments_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Table: discounts (Mã giảm giá)
CREATE TABLE discounts (
                           id bigint NOT NULL,
                           code varchar(50) NOT NULL,
                           discount_percent numeric(5,2) CHECK (discount_percent >= 0 AND discount_percent <= 100),
                           start_date timestamp NOT NULL,
                           end_date timestamp NOT NULL,
                           created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                           updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE discounts
    ADD CONSTRAINT discounts_pkey PRIMARY KEY (id),
    ADD CONSTRAINT discounts_code_key UNIQUE (code);

-- Table: discount_products (Áp dụng giảm giá cho sản phẩm)
CREATE TABLE discount_products (
                                   discount_id bigint,
                                   product_id bigint
);

ALTER TABLE discount_products
    ADD CONSTRAINT discount_products_pkey PRIMARY KEY (discount_id, product_id),
    ADD CONSTRAINT discount_products_discount_id_fkey FOREIGN KEY (discount_id) REFERENCES discounts(id) ON DELETE CASCADE,
    ADD CONSTRAINT discount_products_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

-- Table: reviews (Đánh giá sản phẩm)
CREATE TABLE reviews (
                         id bigint NOT NULL,
                         product_id bigint,
                         user_id bigint,
                         rating integer CHECK (rating >= 1 AND rating <= 5),
                         comment text,
                         created_at timestamp DEFAULT CURRENT_TIMESTAMP,
                         updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id),
    ADD CONSTRAINT reviews_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    ADD CONSTRAINT reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;