export default {
    name: 'titleMixin',
    data: () => ({
        // in case later components do not set or use mixin, save for restoring
        originalPageTitle: null,
    }),
    created() {
        this.originalPageTitle = document.title;
        if (this.pageTitle) {
            document.title = this.pageTitle;
        }
    },
    watch: {
        pageTitle() {
            document.title = this.pageTitle;
        },
    },
    beforeDestroy() {
        document.title = this.originalPageTitle;
    },
};
