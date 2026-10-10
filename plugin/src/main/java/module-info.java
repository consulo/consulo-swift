/**
 * @author VISTALL
 * @since 2026-05-19
 */
module consulo.swift {
    requires consulo.application.api;

    requires consulo.swift.bitcode;
    requires consulo.swift.module.decompiler;

    requires consulo.swift.toolchain;
    requires consulo.swift.packageManager;
    requires consulo.swift.execution;
}