# AWS Setup for NotifyEngine

NotifyEngine uses Amazon SES to send email notifications. This guide walks a new developer through the one-time setup required before running the app locally or in a non-production environment.

---

## 1. Install the AWS CLI

Follow the [official installation guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) for your OS, or use the quick install below.

**macOS (Homebrew)**
```bash
brew install awscli
```

**Linux**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

Verify:
```bash
aws --version
# aws-cli/2.x.x ...
```

---

## 2. Configure credentials

```bash
aws configure
```

You will be prompted for four values:

| Prompt | Value |
|--------|-------|
| AWS Access Key ID | Your IAM user access key |
| AWS Secret Access Key | Your IAM user secret key |
| Default region name | `us-east-1` |
| Default output format | `json` |

This writes to `~/.aws/credentials` and `~/.aws/config`. The app picks these up automatically via the AWS SDK default credentials provider chain — no extra configuration needed.

> **IAM minimum permissions:** The IAM user or role needs `ses:SendEmail` and `ses:VerifyEmailIdentity` on the relevant SES resources. For local dev, attaching the managed policy `AmazonSESFullAccess` is acceptable; use a scoped policy in shared or production environments.

---

## 3. Verify an email identity in SES

SES requires all sender (and, in sandbox mode, recipient) addresses to be verified before sending mail.

### Verify the from-address

```bash
aws ses verify-email-identity \
  --email-address sabat.rajendra3@outlook.com \
  --region us-east-1
```

AWS sends a verification link to that address. Click it. The identity status moves from **Pending** to **Verified**.

Check status:
```bash
aws ses get-identity-verification-attributes \
  --identities sabat.rajendra3@outlook.com \
  --region us-east-1
```

Expected output:
```json
{
  "VerificationAttributes": {
    "sabat.rajendra3@outlook.com": {
      "VerificationStatus": "Success"
    }
  }
}
```

---

## 4. Sandbox mode limitations

New AWS accounts start in **SES sandbox mode**. In sandbox mode:

- You can only send **to** verified email addresses (not arbitrary recipients).
- Sending quota is limited to **200 emails per 24 hours** at **1 email per second**.

To send to an unverified recipient address during local testing, verify that address too:

```bash
aws ses verify-email-identity \
  --email-address <recipient@example.com> \
  --region us-east-1
```

To request production access (exits sandbox), open a support case in the AWS console under **Service Quotas → Amazon SES → Sending quota**. This is not required for local development.

---

## 5. Environment variables

The app reads credentials from the AWS SDK default chain (env vars take precedence over `~/.aws/credentials`). Set these when running outside of a local AWS profile (e.g., Docker, CI):

| Variable | Description |
|----------|-------------|
| `AWS_ACCESS_KEY_ID` | IAM access key |
| `AWS_SECRET_ACCESS_KEY` | IAM secret key |
| `AWS_SESSION_TOKEN` | Only needed for temporary/STS credentials |

Two Spring properties control SES behavior and can be overridden via environment variables:

| Property | Env override | Default | Description |
|----------|-------------|---------|-------------|
| `aws.region` | `AWS_REGION` | `us-east-1` | AWS region for the SES client |
| `aws.ses.from-email` | `AWS_SES_FROM_EMAIL` | `sabat.rajendra3@outlook.com` | Verified sender address |

### Local run (shell export)

```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=us-east-1

./mvnw spring-boot:run
```

### Docker Compose

Add these to the `app` service's `environment` block in `docker-compose.yml`:

```yaml
environment:
  AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
  AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
  AWS_REGION: us-east-1
```

---

## Quick checklist

- [ ] AWS CLI installed and `aws --version` returns output
- [ ] `aws configure` completed with `us-east-1` as the default region
- [ ] From-address verified in SES (`VerificationStatus: Success`)
- [ ] Recipient address(es) verified (required while in sandbox mode)
- [ ] Credentials available to the app (profile or env vars)
