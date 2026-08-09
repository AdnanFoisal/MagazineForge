# MagazineForge: A Beginner's Guide to Getting the Magazine You Wanted

This guide is about one thing: how to ask for a magazine and actually get it.

Most disappointing issues are not a bug. They come from a prompt that was
shorter than the app needed, or from skipping the one screen that exists
specifically so you can catch a misunderstanding before ten minutes of
generation commits to it.

Read the Full AI Mode walkthrough and the prompting section. That is 90% of it.

---

## 1. The two modes

Both live behind the tiles on the Home screen.

| | Full AI Mode | Assisted Mode |
|---|---|---|
| You give it | One description of the issue | A structure, page by page |
| It decides | Article topics, titles, images, page types | Only the writing inside pages you defined |
| Confirmation steps | Two (Intent, then Brief) | None, it builds immediately |
| Best for | "Make me an issue about X" | You already know the contents |

**Full AI Mode** is the one to learn. It is also the only mode with the
Intent gate, which is the feature that stops the app misreading you.

**Assisted Mode** trades that safety for control. You name every page and
the AI only writes the body copy. Nothing is proposed for your approval,
so what you type is what you get.

---

## 2. Before your first issue: Settings

Open Settings once and fill these in. Nothing works without the first two.

**User Profile.** Your name, 7 characters max. It only feeds the greeting.

**LLM Configuration (LiteLLM).** Base URL and Master Key. Tap **Verify &
Save**; do not trust it until the check passes. Everything written in the
magazine comes through here.

**Image API keys.** Pixabay and Pexels, then **Verify & Save Keys**. Skip
this and every photo falls back to a small built-in stock set, which is the
single most common cause of "the pictures have nothing to do with my topic."

**Visual Style.** Auto, Editorial, Modern, or Technical.

- **Auto** (recommended) lets the AI pick the register from what you asked
  for, and you can still change it on the Intent card per issue.
- The other three override it permanently until you change it back.

Visual Style only selects fonts and page geometry. It never changes what
gets written. Editorial is formal and long-form, Modern is bold and
consumer, Technical is dense and data-heavy.

---

## 3. Full AI Mode, screen by screen

### Screen 1: The prompt box

Type what the issue is about. The box starts empty on purpose. Then set:

**Length**

| Setting | Articles |
|---|---|
| Auto | AI decides |
| Short | 3 |
| Medium | 8 |
| Long | 13 |

Longer issues need broader prompts. Asking for 13 articles on one narrow
subject forces the AI to pad, and pads read like padding.

**Cover Image / Back Cover Image** (optional). Paste a URL or a Google
Drive link, or upload. Leave blank and the app finds photos itself. A
supplied cover is used exactly as given and is never reused elsewhere in
the issue.

**Interior Paper Tone.** Cream, Bone, Dark, or White. The page background
behind the text. Cream and Bone read like print; Dark is a night-mode look.

Then tap **Generate Brief**. This does not build anything yet. It reads
your prompt.

### Screen 2: Check the Intent (the important one)

This is the app showing you its understanding, as editable text, before
committing. Every field here is yours to correct, and everything downstream
is built from what you confirm, not from your original prompt.

If you read one section of this guide, read this one. A wrong subject here
becomes a wrong magazine ten minutes later.

**What we'll build (full expansion).** The whole plan in plain language.
Edit this and the entire issue follows your version. If it describes a
different magazine than the one in your head, fix it here.

**Subject.** What the issue is about. Must be filled in or you cannot
continue.

**Audience.** Who it is written for. This moves the writing more than
people expect. "Fifteen-year-olds new to the topic" and "career
specialists" produce genuinely different prose from the same subject.

**Language.** `en` by default. Set the language you want the issue written
in.

**Must cover.** One item per line. These are promises: each becomes
something the issue has to address. Use it for the specific things you
would be annoyed to see missing.

**Avoid.** One item per line. Angles, clichés, or subtopics to stay off.

**Photo subjects.** One per line, and the field most worth your attention.
Each page is matched to whichever of these it is about, so name things a
camera can point at. "A red Ferrari F40", "an espresso machine pulling a
shot", "Kyoto temple in autumn" all work. "Innovation", "the golden era",
"passion" do not, because no photograph is of those, and that is how you
end up with a tiger on the cover of a car magazine.

**Visual Register.** Editorial, Modern, or Technical, pre-selected from
your prompt. Overrides Settings for this issue only.

Then **Looks good**. Or **Back to the prompt** to rewrite from scratch.

If a warning banner appears saying the prompt could not be read
automatically, nothing below it was understood. Fill the fields in
yourself before continuing, or go back and write a longer prompt.

### Screen 3: The Generation Brief

The second confirmation. The AI has now planned the issue and shows you:

- **Category** and **Tone** it chose
- **Layout Density** (image-heavy, balanced, or text-heavy), which sets how
  many words each article gets
- **Cover Title — tap to choose**, three proposed titles as radio buttons
- **Articles**, the list of topics it intends to write

**Pick your cover title here.** Three options are proposed and one is
pre-selected at random. Whichever is selected when you tap the button is
what gets printed. If you dislike all three, go back and generate the brief
again for three new ones.

If the article list is wrong, go back rather than continuing. Fixing the
brief costs seconds; regenerating a finished issue costs the full run.

**Customize Sections ▾** opens the Section Composer, covered in section 5.

Then **Generate Full Issue**. This is the long step. Progress is tracked and
you can leave the screen; the Home screen keeps a **Continue Editing** card
showing how many articles are done.

---

## 4. Assisted Mode, screen by screen

Switch with the **Assisted Mode** tab at the top of the editor.

**Magazine Overall Theme.** The frame for the whole issue. Every page is
written knowing this.

**FRONT COVER.** What should be written on the cover, and optionally an
image URL or Google Drive link.

Then add pages. Each **Article** page takes:

- **Topic** — what this page is about. Be specific; this is the entire
  brief for that page.
- **Image URL** — optional. Given one, it is used exactly. Left blank, the
  app searches for a photo matching the topic.
- **Tone** and **Layout Density** per page.

**Back Cover** adds a closing page.

Then **Generate Custom Issue**.

Two things to know about this mode:

- There is no Intent card and no brief to approve. It builds straight away,
  so re-read your page topics before tapping.
- Per-page Tone and Layout Density are passed inside the composed prompt
  rather than as separate settings, so treat them as strong requests rather
  than switches.

---

## 5. Section Composer

Available from **Customize Sections ▾** in both modes. Toggles for the
furniture around your articles:

| Toggle | Adds |
|---|---|
| **Masthead** | An editor's-letter opening page. Takes an angle, e.g. "Founder's Note" |
| **Sidebar (Per Article)** | A boxed aside on each article. Takes a topic, e.g. "Key Takeaways" |
| **Pull Quotes** | Large lifted quotes inside articles |
| **Back Cover** | A closing page |
| **TOC Teasers** | One-line descriptions in the contents |
| **Article Bylines** | Author lines under headlines |

Sidebars and pull quotes break up long text, so they earn their place on
text-heavy issues. On a short image-heavy issue they crowd the page.

Tap **Apply Structure** to save.

---

## 6. How to write a prompt that works

The mechanism worth understanding: your prompt is read once into a
structured plan, you confirm that plan, and every article and photo search
is anchored to it. A vague prompt produces a vague plan, and a vague plan
cannot be rescued later.

**Name the specifics.** The single highest-value habit. Every proper noun
you supply is one the AI does not have to invent, and inventions are where
issues drift.

Weak: `a magazine about cars`

Strong: `a history of supercars, covering Ferrari, Lamborghini, BMW,
Mercedes and McLaren, one article per marque, focused on their defining
models and the engineering that made them`

**Say who it is for.** "for readers who already restore cars" and "for
someone who has never been to a car show" are different magazines.

**Say what must appear.** Anything you would be annoyed to see missing
belongs in the prompt, then confirmed under Must cover.

**Say what to avoid.** Effective against clichés: "avoid ranking lists",
"avoid speculation about upcoming models".

**Describe photographable things.** Repeating this because it is the most
common failure: photo searches match subjects a camera can point at.
"A red Ferrari F40 on a track" finds a car. "The passion of Italian
engineering" finds whatever the stock library associates with passion.

**Match length to breadth.** Long is 13 articles. Give it enough subject
to fill them.

### A worked example

Typed into the prompt box:

```
A history of supercars covering Ferrari, Lamborghini, BMW, Mercedes and
McLaren. One article per marque, each on its defining model and the
engineering behind it. For readers who know cars but not their history.
Avoid ranking lists and avoid anything about upcoming releases.
```

Length: Medium. On the Intent card, expect roughly:

- **Subject** — a history of supercars across five marques
- **Audience** — enthusiasts familiar with cars, new to their history
- **Must cover** — Ferrari, Lamborghini, BMW, Mercedes, McLaren
- **Avoid** — ranking lists, upcoming releases
- **Photo subjects** — the marques and their models, one per line
- **Visual Register** — Editorial or Modern

Then correct anything off, confirm, choose your cover title, and generate.

---

## 7. When the result is wrong

**The photos do not match the topic.** Check that Pixabay and Pexels keys
are saved and verified in Settings. Then check Photo subjects on the Intent
card: replace anything abstract with something a camera can point at.

**The same photo appears on several pages.** Fixed. Every slot in an issue
now claims its photo, so no two pages share one unless the entire available
pool is exhausted. If you still see it, your subject is narrow enough that
the providers ran out of distinct results — broaden the Photo subjects
lines.

**The cover and back cover match.** Same fix, same cause.

**Every issue on a topic has the same cover title.** Fixed. The three
proposed titles on the brief are now selectable, and a different one is
pre-selected each time. Pick deliberately.

**The articles are not about what I asked.** The Intent card is the place
to catch this. If its expansion was right and the articles still drifted,
tighten Must cover with the specifics.

**The writing is generic.** Set Audience to someone specific and put the
clichés you are seeing into Avoid.

**Pages look empty or overfull.** Layout Density on the brief governs word
count per article. Text-heavy fills pages, image-heavy leaves room for
photographs.

---

## 8. Reference

**Screen order in Full AI Mode**
Home → prompt box → Check the Intent → Generation Brief → generation →
your issue.

**What each control actually changes**

| Control | Affects |
|---|---|
| Visual Style / Visual Register | Fonts and layout only, never the words |
| Interior Paper Tone | Page background colour only |
| Length | Number of articles (3 / 8 / 13 / auto) |
| Layout Density | Words per article |
| Audience | Vocabulary and assumed knowledge |
| Must cover | Topics the issue has to address |
| Avoid | Topics and angles it must not |
| Photo subjects | What the photo search looks for |

**Library.** Finished issues are on the Home screen under Your Library, and
**View all** opens the full list. Tap any cover to read it.

**Continue Editing.** An interrupted run leaves a card on Home with its
progress. Tap to pick it up.

**Two rules worth keeping**

1. Never skip past the Intent card. It is five seconds against a ten-minute
   run built on a misunderstanding.
2. Name things a camera can see.

