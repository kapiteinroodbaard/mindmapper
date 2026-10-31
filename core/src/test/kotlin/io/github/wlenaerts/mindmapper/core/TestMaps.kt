package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.layout.Size
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic

/** Fixed-size measurement so layout tests do not depend on fonts. */
val fixedMeasure: (Topic, Int) -> Size = { _, depth -> if (depth == 0) Size(120f, 60f) else Size(80f, 30f) }

fun topic(id: String, vararg children: Topic) = Topic(id = id, text = id, children = children.toList())

/** A topic [id] with [count] leaf children named `<id>.<n>`. */
fun branch(id: String, count: Int) = Topic(id = id, text = id, children = List(count) { Topic(id = "$id.$it", text = "$id.$it") })

fun map(vararg mains: Topic, layout: LayoutKind = LayoutKind.RADIAL) =
    MindMap(layout = layout, root = Topic(id = "root", text = "root", children = mains.toList()))
