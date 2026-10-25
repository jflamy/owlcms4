# Windows Code Signing for controlpanel and video

## Context

The Windows executables of `owlcms/controlpanel` (`owlcms.exe`) and `owlcms/video` (`video_windows.exe`) are cross-compiled on `ubuntu-latest` with `fyne-cross windows` and are currently **not Authenticode-signed**. Unsigned Go binaries are frequently flagged by antivirus heuristics and by SmartScreen.

### Build flags are not the fix

A common suggestion is to "add versioninfo" and build with `go build -ldflags="-s -w" -trimpath`. Assessment:

- **Version info**: already present. `fyne-cross windows` runs `fyne package`, which embeds an icon, a manifest, and a VERSIONINFO resource (ProductName, ProductVersion, FileDescription, FileVersion). CompanyName and LegalCopyright are not set; adding them is at best a marginal heuristic improvement.
- **`-s -w`** (strip symbols/DWARF): no reliable benefit. Stripped and obfuscated Go binaries are common in malware, so some engines score them as *more* suspicious.
- **`-trimpath`**: removes build paths only. Useful for reproducibility, no antivirus effect.
- **macOS/Linux**: not concerned. macOS relies on signing and notarization, which the controlpanel workflow already does.

The effective measures are **Authenticode signing** (reputation accrues to the certificate) and submitting false positives to the Microsoft WDSI portal.

## Options considered

| | Azure Artifact Signing | SignPath Foundation (free OSS) |
|---|---|---|
| Publisher shown | Validated legal name (individual or organization) | "SignPath Foundation" |
| Per-release approval | None, fully automated | Mandatory manual approval in the SignPath UI |
| Cost | About US$10/month (Basic, 5000 signatures) | Free |
| Requirements | Paid Azure subscription, identity validation | OSI license on all components, code signing policy page, team roles, MFA, product name/version metadata enforcement |
| Notes | — | `video` has no license file; FyneApp.toml versions would need to match the tag |

**Decision: Azure Artifact Signing** (formerly *Azure Trusted Signing*). It keeps `release.sh` fully automated and shows "Jean-François Lamy" (or later an organization) as the publisher.

### No EV certificate, and EV no longer matters for SmartScreen

- Artifact Signing does **not** issue Extended Validation (EV) certificates, and Microsoft says there are no plans to.
- Microsoft's SmartScreen documentation states that **EV certificates no longer bypass SmartScreen**. OV and EV certificates are treated the same: a newly signed file shows the "unrecognized app" warning, with the verified publisher name, until the file hash or the certificate builds download reputation. Microsoft adds that paying for EV only to avoid SmartScreen warnings is no longer justified.
- EV is still required for kernel-mode drivers submitted through the Windows Hardware Dev Center, and some enterprise procurement policies ask for it. Neither applies here.
- Faster ways to clear warnings: submit each new signed release through the Microsoft Security Intelligence portal (<https://www.microsoft.com/wdsi>), or distribute through the Microsoft Store (Store apps are re-signed by Microsoft and never warn).

## Eligibility (individual)

- Public Trust certificates for **individual developers** are available only in the **US or Canada**. Organizations are accepted in the US, Canada, EU, UK, Australia, New Zealand, Japan, South Korea, Singapore, Switzerland, Norway, and Israel.
- Requires a **paid** Azure subscription. Free, trial, and sponsored subscriptions (including Visual Studio credit subscriptions) are refused.
- The Azure **billing account** must have **Account type = Individual**. Its **legal name and sold-to address must match the government ID exactly**, including accents ("Jean-François Lamy").
- Identity documents:
  - Government-issued photo ID: passport, driver's licence, or ID card.
  - Possibly proof of address: a utility bill or bank statement less than 3 months old, with name, address, and date on the first page.
- The check is done by **AU10TIX** through a phone camera and an email PIN. Verification links expire after 7 days.
- Processing takes **1 to 20 business days**.
- The certificate shows the validated name, and probably the city, province, and country, but not the street address. The portal shows a subject preview before you commit.

### Existing accounts (checked 2026-10-25)

- Azure DevOps organization `jflamy` (signed in as `lamyjeanfrancois@outlook.com`): billing was never set up (free tier only), so there is no reusable paid subscription.
- The Azure portal, signed in as the gmail account (directory `lamyjeanfrancoisgmailcom.onmicrosoft.com`), lists no subscriptions.
- Before creating a new subscription, check:
  - **Switch directories**.
  - The "subscriptions with billing access" link.
  - The outlook.com account.
- Use a single Microsoft account for everything.

## Setup steps

1. **Subscribe (pay-as-you-go)**: <https://azure.microsoft.com/pricing/purchase-options/pay-as-you-go>
   - Choose **Individual**.
   - Enter the legal name exactly as on the ID, and the current address.
   - A credit card and phone verification are required.
2. **Register the resource provider**: <https://portal.azure.com/#view/Microsoft_Azure_Billing/SubscriptionsBladeV2>
   - Open the subscription, then **Resource providers**, then `Microsoft.CodeSigning`, then **Register**.
   - Record the **Subscription ID**.
3. **Create the Artifact Signing account**: search the portal for "Artifact Signing", then **Create**.
   - Resource group: for example `owlcms-signing`.
   - Name: for example `owlcms`.
   - Region, and the **Basic** tier.
   - Record the **account name** and the **Account URI** (for example `https://eus.codesigning.azure.net/`). Billing starts here.
4. **Grant yourself the verifier role**:
   - Signing account, then **Access control (IAM)**, then **Add role assignment**.
   - Pick **Artifact Signing Identity Verifier** and assign it to yourself.
5. **Identity validation**:
   - Signing account, then **Identity validations**, then **Individual**, then **New identity**, then **Public**.
   - Complete the AU10TIX flow and wait for the status **Completed**.
6. **Certificate profile**:
   - Signing account, then **Certificate profiles**, then **Create**, then **Public Trust**, and select the validated identity.
   - Record the **profile name**.
7. **App registration for GitHub OIDC**: <https://entra.microsoft.com>
   - **Applications**, then **App registrations**, then **New registration**. Name: `github-owlcms-signing`.
   - Record the **Application (client) ID** and the **Directory (tenant) ID**.
   - **Certificates & secrets**, then **Federated credentials**, then **Add credential**. Scenario: *GitHub Actions deploying Azure resources*.
     - Organization `owlcms`, repository `controlpanel`, entity **Environment**, value `release`.
     - Same again for repository `video`.
8. **Grant the signer role to the app**:
   - Certificate profile, then **Access control (IAM)**, then **Add role assignment**.
   - Pick **Artifact Signing Certificate Profile Signer**, with member `github-owlcms-signing`.

With OIDC, **no client secret or key is generated**; only identifiers are needed.

## GitHub configuration

Set the values once at the `owlcms` organization level:

- Secrets: <https://github.com/organizations/owlcms/settings/secrets/actions>
- Variables: <https://github.com/organizations/owlcms/settings/variables/actions>

| Name | Value | Source | Type |
|---|---|---|---|
| `AZURE_TENANT_ID` | Directory (tenant) ID | step 7 | secret |
| `AZURE_CLIENT_ID` | Application (client) ID | step 7 | secret |
| `AZURE_SUBSCRIPTION_ID` | Subscription ID | step 2 | secret |
| `AZURE_SIGNING_ENDPOINT` | host of the Account URI, for example `eus.codesigning.azure.net` | step 3 | variable |
| `AZURE_SIGNING_ACCOUNT` | Artifact Signing account name | step 3 | variable |
| `AZURE_CERT_PROFILE` | certificate profile name | step 6 | variable |

None of these is sensitive by itself: the federated credential only accepts tokens from the `release` environment of the two repositories.

Create an environment named `release` in each repository:

- <https://github.com/owlcms/controlpanel/settings/environments>
- <https://github.com/owlcms/video/settings/environments>

## Workflow integration (to implement once the profile is active)

The official `azure/artifact-signing-action` runs **only on Windows runners**. Because the Windows executables are built on Ubuntu with fyne-cross, use **jsign** instead. jsign is cross-platform and supports Artifact Signing with `--storetype TRUSTEDSIGNING`, so signing happens in the existing job.

Sketch for `build_windows_crosscompile` in `controlpanel/.github/workflows/release.yaml`. The same applies to `build_windows` in `video`, with `artifacts/video_windows.exe`.

```yaml
  build_windows_crosscompile:
    runs-on: ubuntu-latest
    environment: release
    permissions:
      id-token: write
      contents: read

    steps:
    # ... existing checkout / fyne-cross build producing artifacts/owlcms.exe ...

    - name: Azure login
      uses: azure/login@v3
      with:
        client-id: ${{ secrets.AZURE_CLIENT_ID }}
        tenant-id: ${{ secrets.AZURE_TENANT_ID }}
        subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}

    - name: Install jsign
      run: |
        curl -fsSL -o jsign.deb https://github.com/ebourg/jsign/releases/download/7.6/jsign_7.6_all.deb
        sudo apt-get install -y ./jsign.deb

    - name: Sign Windows executable
      run: |
        TOKEN=$(az account get-access-token --resource https://codesigning.azure.net --query accessToken -o tsv)
        echo "::add-mask::$TOKEN"
        jsign --storetype TRUSTEDSIGNING \
              --keystore ${{ vars.AZURE_SIGNING_ENDPOINT }} \
              --storepass "$TOKEN" \
              --alias "${{ vars.AZURE_SIGNING_ACCOUNT }}/${{ vars.AZURE_CERT_PROFILE }}" \
              artifacts/owlcms.exe

    # ... existing upload-artifact step ...
```

Notes:

- Artifact Signing certificates are valid for only **3 days**, so timestamping is mandatory. jsign enables it automatically for this storetype.
- When several files are signed, pass them all to a single jsign call. Each call makes an extra API request, which counts against the quota.
- The `environment: release` setting also allows optional GitHub protection rules, such as required reviewers.

## References

- Artifact Signing quickstart: <https://learn.microsoft.com/azure/artifact-signing/quickstart>
- Artifact Signing FAQ: <https://learn.microsoft.com/azure/artifact-signing/faq>
- SmartScreen reputation for Windows app developers: <https://learn.microsoft.com/windows/apps/package-and-deploy/smartscreen-reputation>
- Official GitHub Action (Windows runners only): <https://github.com/Azure/artifact-signing-action>
- jsign: <https://ebourg.github.io/jsign/>
- SignPath Foundation conditions: <https://signpath.org/terms>
- SignPath GitHub integration: <https://docs.signpath.io/trusted-build-systems/github>
