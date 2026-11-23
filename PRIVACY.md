# AloudRite privacy notice

AloudRite is published by Ritenoar CommV, a company in Belgium. You reach it at nora+aloud@ritenoar.com. This notice
says which data the plugin handles, as section 16.5 of the JetBrains Developer Agreement asks of a developer.

## What the plugin collects

The plugin collects no personal, statistical or telemetric data. It sends nothing to its developer, and it has no
account, no analytics and no crash report. The text you have it read is turned into speech on your machine and goes
nowhere else.

## The only network requests

The only network requests the plugin makes are the downloads of its models, and only after you agreed to the download
in the question the plugin asks first. The purpose of a download is to get the speech model that a reading needs and the
aligner model that the highlight of the word needs. They come from Hugging Face, and from the content network that
Hugging Face redirects to.

Hugging Face and that content network receive what any download shows to a server, such as your IP address and the
address of the file you ask for. They handle it under their own privacy policy, which is at
https://huggingface.co/privacy. The plugin sends them no other data, and Ritenoar CommV receives none of it.

## What is stored on your machine

* The models, one folder for each, in one folder per user: `$XDG_DATA_HOME/aloudrite`, or `~/.local/share/aloudrite` where
  that is not set, on Linux, and `~/Library/Application Support/AloudRite` on macOS. Delete a folder to remove its model
* The settings of the plugin, which are the reading speed, the model and the voice, in the settings of your IDE. They
  never leave your machine

## How to avoid the download

Decline when the plugin asks whether to download a model, and the plugin makes no request. To read with a model
nonetheless, put it in place by hand in the folder above, as the [README](README.md) describes under "The speech model".
A model that is in place is used as it is, and the plugin asks nothing.

## Objecting

Since the plugin gives no data to Ritenoar CommV, there is nothing at Ritenoar CommV to object to. For the data that
Hugging Face receives, object to Hugging Face under its privacy policy, or avoid the download as above. Write to the
address above with any question.
