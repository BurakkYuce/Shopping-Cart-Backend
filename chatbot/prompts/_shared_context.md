## Anaphora and demonstrative vocabulary (shared)

Tokens below signal that the current message refers back to something the
assistant already produced. When any of these appear AND the **Recent
conversation context** block has a non-empty `ASSISTANT:` turn, treat the
message as a follow-up rather than a standalone question.

### Turkish demonstratives and anaphora

- Inflected / plural demonstratives (unambiguously anaphoric):
  `bunlar`, `bunları`, `bunların`, `bunun`, `bundan`, `bunu`,
  `şunlar`, `şunları`, `şunun`, `şunu`,
  `onu`, `onları`, `onların`
- Relative-clause nominalizers (always anaphoric — "the ones that were …"):
  `olanlar`, `olanı`, `olanları`, `olanların`
- Demonstrative-noun / selection references:
  `yukarıdakiler`, `ilki`, `sonuncusu`, `seçtiklerim`,
  `aynı`, `aynı veriler`, `aynı sonuç`, `ilk sıradaki`, `son sıradaki`
- Bare `bu`, `şu`, `o`, `onlar` are also demonstratives but they appear
  frequently in standalone timeframes ("bu ay", "şu anda"). Treat them as
  anaphoric ONLY when they clearly point at a prior assistant turn.

### English demonstratives and anaphora

- Explicit plurals / pronouns (unambiguously anaphoric):
  `these`, `those`, `them`
- Multi-word references:
  `the first`, `the last`, `the same`, `the same data`, `the above`,
  `just the ones`, `only the ones`, `only active ones`, `only the active ones`
- Bare `this`, `that`, `it` are ambiguous — treat as anaphoric only with
  clear referent in the prior turn.

### Examples that are follow-ups (when a prior assistant data turn exists)

- "bu ürünün kategorisi nedir" → refers to previously listed product
- "bunların toplam geliri" → refers to previously listed rows
- "bu verileri grafikle göster" → re-plot prev result
- "ilk sıradakinin stok miktarı" → refers to the first row of the prior table
- "sadece aktif olanlar" → filter the prior result set to active items
- "same thing for last month" → reuse prior query shape with new window
- "show them on a chart" → visualise prior data
- "what about the first one" → drill into the top row of the prior result

### When no prior turn exists

If the message contains an anaphoric token but there is no prior assistant
data turn to anchor it, fall through to `clarify` instead of forcing a
follow-up interpretation.
