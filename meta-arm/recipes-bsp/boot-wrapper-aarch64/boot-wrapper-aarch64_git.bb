SUMMARY = "Linux aarch64 boot wrapper with FDT support"
LICENSE = "BSD"

LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=bb63326febfb5fb909226c8e7ebcef5c"

SRC_URI = "git://git.kernel.org/pub/scm/linux/kernel/git/mark/boot-wrapper-aarch64.git"
SRCREV = "8d5a765251d9113c3c0f9fa14de42a9e7486fe8a"

PV = "git${SRCPV}"

S = "${WORKDIR}/git"

inherit autotools deploy

PACKAGE_ARCH = "${MACHINE_ARCH}"

COMPATIBLE_MACHINE ?= "invalid"

# Device tree to put in the image
# by default use the standard kernel devicetree
# This should be overwritten if the devicetree is not generated
# by the kernel.
# This should point to a file in the deploy image directory
BOOT_WRAPPER_AARCH64_DEVICETREE ??= "${KERNEL_DEVICETREE}"

# Kernel image to put in the image
# This should point to a file in the deploy image directory
BOOT_WRAPPER_AARCH64_KERNEL ??= "Image"

# Kernel command line for the image
BOOT_WRAPPER_AARCH64_CMDLINE ??= "rw"

# Image generated by boot wrapper
BOOT_WRAPPER_AARCH64_IMAGE ??= "linux-system.axf"

DEPENDS += "virtual/kernel dtc-native"

EXTRA_OECONF += "--with-kernel-dir=${WORKDIR}/kernel"
EXTRA_OECONF += "--with-dtb=${WORKDIR}/kernel/dummy.dtb"
EXTRA_OECONF += "--with-cmdline=\"\""
EXTRA_OECONF += "--enable-psci --enable-gicv3"

# unset LDFLAGS solves this error when compiling kernel modules:
# aarch64-poky-linux-ld: unrecognized option '-Wl,-O1'
EXTRA_OEMAKE += "'LDFLAGS= --gc-sections '"

# Strip prefix if any
REAL_DTB = "${@os.path.basename(d.getVar('BOOT_WRAPPER_AARCH64_DEVICETREE'))}"

EXTRA_OEMAKE += "'KERNEL_DTB=${DEPLOY_DIR_IMAGE}/${REAL_DTB}'"
EXTRA_OEMAKE += "'KERNEL_IMAGE=${DEPLOY_DIR_IMAGE}/${BOOT_WRAPPER_AARCH64_KERNEL}'"
EXTRA_OEMAKE += "'CMDLINE=${BOOT_WRAPPER_AARCH64_CMDLINE}'"


do_configure_prepend() {
    # Create dummy files to make configure happy.
    # We will pass the generated ones directly to make.
    mkdir -p ${WORKDIR}/kernel/arch/arm64/boot
    echo "dummy" > ${WORKDIR}/kernel/arch/arm64/boot/Image
    echo "dummy" > ${WORKDIR}/kernel/dummy.dtb

    # Generate configure
    (cd ${S} && autoreconf -i || exit 1)
}

do_compile[noexec] = "1"
do_install[noexec] = "1"

# We need the kernel to create an image
do_deploy[depends] += "virtual/kernel:do_deploy"

do_deploy() {
    if [ ! -f ${DEPLOY_DIR_IMAGE}/${REAL_DTB} ]; then
        echo "ERROR: cannot find ${REAL_DTB} in ${DEPLOY_DIR_IMAGE}" >&2
        echo "Please check your BOOT_WRAPPER_AARCH64_DEVICETREE settings" >&2
        exit 1
    fi

    if [ ! -f ${DEPLOY_DIR_IMAGE}/${BOOT_WRAPPER_AARCH64_KERNEL} ]; then
        echo "ERROR: cannot find ${BOOT_WRAPPER_AARCH64_KERNEL}" \
            " in ${DEPLOY_DIR_IMAGE}" >&2
        echo "Please check your BOOT_WRAPPER_AARCH64_KERNEL settings" >&2
        exit 1
    fi

    oe_runmake clean
    oe_runmake all

    install -D -p -m 644 ${BOOT_WRAPPER_AARCH64_IMAGE} \
        ${DEPLOYDIR}/linux-system.axf
}
addtask deploy before do_build after do_compile