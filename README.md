# Range SeekBar

Similar to the Android built-in SeekBar, but it allows to edit a values in a range (start, end).
<br />
![](./art/video.gif)

---

## Installation
### Maven
Add the library dependency:

    implementation 'it.sephiroth.android.library.uigestures:uigesture-recognizer-kotlin:**version**'

### JitPack
**Step 1.** Add the JitPack repository to your build file:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	
**Step 2.** Add the dependency to your project's build.gradle file:

	dependencies {
	        implementation 'com.github.sephiroth74:RangeSeekBar:Tag'
	}

To See the last release version: https://jitpack.io/private#sephiroth74/RangeSeekBar

---

## Usage


      <it.sephiroth.android.library.rangeseekbar.RangeSeekBar
        android:id="@+id/rangeSeekBar"
        style="@style/Base.Sephiroth.Widget.RangeSeekBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginEnd="8dp"
        android:layout_marginRight="8dp"
        android:layout_marginTop="8dp"
        android:max="100"
        app:layout_constraintLeft_toLeftOf="@+id/textView"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/textView"
        app:range_progress_endValue="50"
        app:range_progress_startEnd_minDiff="1"
        app:range_progress_startValue="50" />

To see the list of all the available attributes, see [attrs.xml](./rangeseekbar-library/src/main/res/values/attrs.xml)


---

## License

MIT License

Copyright (c) 2017 Alessandro Crugnola

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---
