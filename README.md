# SMS Sender

A complete working example to supplement the description of `SmsManager`'s
results mechanism given in
[this Stack Overflow answer](https://stackoverflow.com/a/24845193).

## Overview

The design is a modern update to the classic pattern that uses a `Service` to
send messages queued through a `ContentProvider` or local `SQLiteDatabase`.

The database here is managed through Room, and its schema comprises two
entities, `Message` and `SendTask`, with their corresponding DAOs. Each DAO has
functions for the CRUD operations that you would expect, and `MessageDao` also
provides a `Flow` on a query for the oldest queued `Message`, greatly
simplifying the send routine.

That routine is executed in a `Worker` which is started immediately for our
demonstration, but which could easily be scheduled for whenever, with whatever
constraints are needed.

The actual work for the send is handled in `SmsSendRepository`. We use Hilt to
maintain a singleton that we also inject into the statically-registered results
Receiver, allowing us to keep the overall logic in one place.

The UI is done in minimal Compose, and is basically just text logs with a few
buttons.

<p align="center">
<img src="images/screenshots.png"
alt="Screenshots of a send task running, and a failed send."
width="20%" />
</p>

### Fake delivery reports

The last thing to note is the fake delivery report mechanism. It's located in
the debug build, and it only works when the test device is sending to itself.
It's nothing more than a regular `BroadcastReceiver` registered for
`SMS_RECEIVED` that checks each incoming message against those sent, and upon a
match, sends the same kind of broadcast you would get in a real run, complete
with valid result PDU attached as an extra.

This was mainly meant to make testing on the emulators more convenient, but
it'll also work on a real device sending to itself, should the carrier not
provide delivery reports. As is, the app assumes that it'll be running on a
single default emulator; i.e., it sends messages to hard-coded port number 5554.

---

<br />

MIT License

Copyright (c) 2024 Mike M.

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.