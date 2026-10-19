-- Stores all donation transactions made through Razorpay.
-- Each donation links to the user who made it and tracks the Razorpay order/payment lifecycle.
CREATE TABLE donations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount              DECIMAL(10,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    razorpay_order_id   VARCHAR(255) UNIQUE NOT NULL,
    razorpay_payment_id VARCHAR(255) UNIQUE,
    status              VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMP DEFAULT NOW()
);

-- Index for fetching a user's donation history.
CREATE INDEX idx_donations_user_id ON donations(user_id);

-- Index for looking up donations by Razorpay order ID during payment verification.
CREATE INDEX idx_donations_razorpay_order ON donations(razorpay_order_id);
