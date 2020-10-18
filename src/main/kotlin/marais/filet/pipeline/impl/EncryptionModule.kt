package marais.filet.pipeline.impl

import marais.filet.pipeline.BytesModule
import marais.filet.pipeline.Context
import java.nio.ByteBuffer

/**
 * TODO encryption module
 */
class EncryptionModule : BytesModule {

    override fun processIn(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf

    override fun processOut(ctx: Context, buf: ByteBuffer): ByteBuffer? = buf
}
